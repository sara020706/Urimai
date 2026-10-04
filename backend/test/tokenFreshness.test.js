/**
 * Regression test: a freshly issued token must never be revoked.
 *
 * `jwt.sign` floors `iat` to a whole second, while `users.token_valid_from` has
 * millisecond precision. Comparing them naively meant a user who signed up at
 * 12:00:00.150 received a token stamped 12:00:00.000 — 150ms *before* their own
 * account row — and `loadUser` revoked it on the very next request. It hit
 * roughly one signup in six, so it looked like flaky infrastructure rather than
 * a bug.
 *
 * The second half of this file proves the fix did not weaken revocation, which
 * is the property the comparison exists for.
 *
 *   TEST_PORT=4199 node test/tokenFreshness.test.js
 */

require('dotenv').config();
const http = require('http');
const pool = require('../src/db/pool');

const PORT = Number(process.env.TEST_PORT || 4199);
const STAMP = `tok${Date.now()}`;
const ITERATIONS = 25;

let passed = 0;
let failed = 0;
const failures = [];

function check(label, ok, detail) {
  if (ok) {
    passed += 1;
    console.log(`  PASS  ${label}`);
  } else {
    failed += 1;
    failures.push(label + (detail ? ` — ${detail}` : ''));
    console.log(`  FAIL  ${label}${detail ? ` — ${detail}` : ''}`);
  }
}

function request(method, path, { token, body } = {}) {
  return new Promise((resolve, reject) => {
    const payload = body ? Buffer.from(JSON.stringify(body)) : null;
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (payload) {
      headers['Content-Type'] = 'application/json';
      headers['Content-Length'] = payload.length;
    }
    const req = http.request({ host: 'localhost', port: PORT, path, method, headers }, (res) => {
      let data = '';
      res.on('data', (c) => { data += c; });
      res.on('end', () => {
        let parsed = null;
        try { parsed = data ? JSON.parse(data) : null; } catch (_) { parsed = data; }
        resolve({ status: res.statusCode, body: parsed, raw: data });
      });
    });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function main() {
  console.log(`\nToken freshness (port ${PORT})\n`);

  const created = [];

  // --- A fresh token must work immediately, every time --------------------
  let revoked = 0;
  let otherFailure = null;

  for (let i = 0; i < ITERATIONS; i += 1) {
    const username = `${STAMP}u${i}`;
    const signup = await request('POST', '/auth/signup', {
      body: { username, password: 'test1234', displayName: 'Token Test' }
    });
    if (signup.status === 429) {
      throw new Error(
        'Rate limited. Start the server with `npm run dev:test` before running this suite.'
      );
    }
    if (signup.status !== 201) {
      otherFailure = `signup ${signup.status}`;
      break;
    }
    created.push(username);

    // The very next request with the token we were just handed.
    const profile = await request('GET', '/profile', { token: signup.body.token });
    if (profile.status === 401 && profile.body && profile.body.code === 'TOKEN_REVOKED') {
      revoked += 1;
    } else if (profile.status !== 200) {
      otherFailure = `profile ${profile.status}`;
    }
  }

  check(
    `a newly issued token is accepted on the next request (${ITERATIONS} signups)`,
    revoked === 0,
    revoked > 0 ? `${revoked}/${ITERATIONS} were revoked immediately` : ''
  );
  check('no unexpected failures during the run', otherFailure === null, otherFailure || '');

  // --- Revocation must still work -----------------------------------------
  console.log('\n  -- revocation still enforced --');

  const victim = `${STAMP}victim`;
  const signup = await request('POST', '/auth/signup', {
    body: { username: victim, password: 'test1234', displayName: 'Victim' }
  });
  created.push(victim);
  const victimToken = signup.body.token;

  const before = await request('GET', '/profile', { token: victimToken });
  check('the account works before revocation', before.status === 200, `status ${before.status}`);

  // Bump token_valid_from the way blocking an account does. Push it clearly
  // past the allowance so this tests revocation, not the boundary.
  await pool.query(
    `UPDATE users SET token_valid_from = now() + interval '5 seconds' WHERE username = $1`,
    [victim]
  );
  await new Promise((r) => setTimeout(r, 5200));

  const after = await request('GET', '/profile', { token: victimToken });
  check('the old token is rejected after revocation', after.status === 401, `status ${after.status}`);
  check(
    'the refusal names the reason',
    after.body && after.body.code === 'TOKEN_REVOKED',
    after.body ? `code ${after.body.code}` : ''
  );

  const fresh = await request('POST', '/auth/login', {
    body: { username: victim, password: 'test1234' }
  });
  const afterRelogin = await request('GET', '/profile', { token: fresh.body.token });
  check(
    'signing in again issues a working token',
    afterRelogin.status === 200,
    `status ${afterRelogin.status}`
  );

  // --- Cleanup -------------------------------------------------------------
  if (created.length) {
    await pool.query('DELETE FROM users WHERE username = ANY($1)', [created]);
  }

  console.log(`\n${passed} passed, ${failed} failed\n`);
  if (failures.length) {
    console.log('FAILURES:');
    failures.forEach((f) => console.log('  - ' + f));
  }
  await pool.end();
  process.exit(failed === 0 ? 0 : 1);
}

main().catch(async (err) => {
  console.error('\nTest harness error:', err.message);
  try {
    await pool.query(`DELETE FROM users WHERE username LIKE $1`, [`${STAMP}%`]);
  } catch (_) {}
  try { await pool.end(); } catch (_) {}
  process.exit(1);
});
