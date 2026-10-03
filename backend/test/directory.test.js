/**
 * Lawyer directory and proxied-contact test.
 *
 * The privacy claim is that contact details are never browsable: they reach a
 * citizen only after that specific lawyer accepts that specific request. This
 * exercises the real HTTP surface to confirm it, along with the directory
 * filters and the block semantics.
 *
 *   TEST_PORT=4199 node test/directory.test.js
 */

require('dotenv').config();
const http = require('http');
const pool = require('../src/db/pool');

const PORT = Number(process.env.TEST_PORT || 4199);
const STAMP = `dir${Date.now()}`;

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

async function signup(username, role) {
  const res = await request('POST', '/auth/signup', {
    body: { username, password: 'test1234', displayName: username, role }
  });
  if (res.status === 429) {
    throw new Error(
      'Signup was rate limited. These suites create several accounts per run; ' +
      'start the server with RATE_LIMIT_DISABLED=true, or wait 15 minutes.'
    );
  }
  if (res.status !== 201) throw new Error(`signup ${username}: ${res.raw}`);
  return { token: res.body.token, userId: res.body.userId };
}

const SECRET_EMAIL = 'secret.counsel@example.com';
const SECRET_PHONE = '+91-99999-00000';

async function seedLawyer(userId, name, opts) {
  await pool.query(
    `INSERT INTO lawyer_profiles
       (user_id, full_name, bar_council_reg_number, bar_council_state,
        years_experience, specializations, languages, practice_state,
        practice_district, verification_status, contact_email, contact_phone,
        accepting_questions)
     VALUES ($1,$2,$3,'Tamil Nadu',$4,$5,$6,$7,$8,$9,$10,$11,$12)
     ON CONFLICT (user_id) DO UPDATE SET verification_status = EXCLUDED.verification_status`,
    [
      userId, name, `${STAMP}/${userId}`,
      opts.years, opts.specializations, opts.languages,
      opts.state, opts.district, opts.status,
      SECRET_EMAIL, SECRET_PHONE, opts.accepting !== false
    ]
  );
}

async function cleanup(names) {
  const { rows } = await pool.query('SELECT id FROM users WHERE username = ANY($1)', [names]);
  const ids = rows.map((r) => r.id);
  if (ids.length) {
    await pool.query('DELETE FROM admin_audit_log WHERE admin_user_id = ANY($1)', [ids]);
    await pool.query('DELETE FROM users WHERE id = ANY($1)', [ids]);
  }
}

async function main() {
  const names = [
    `${STAMP}citizen`, `${STAMP}citizen2`,
    `${STAMP}verified`, `${STAMP}pending`, `${STAMP}suspended`
  ];

  console.log(`\nDirectory and proxied contact (port ${PORT})\n`);

  const citizen = await signup(names[0], 'USER');
  const citizen2 = await signup(names[1], 'USER');
  const verified = await signup(names[2], 'LAWYER');
  const pending = await signup(names[3], 'LAWYER');
  const suspended = await signup(names[4], 'LAWYER');

  await seedLawyer(verified.userId, `${STAMP} Verified Counsel`, {
    years: 12, specializations: ['Welfare Law', 'Consumer'], languages: ['Tamil', 'English'],
    state: 'Tamil Nadu', district: 'Chennai', status: 'VERIFIED'
  });
  await seedLawyer(pending.userId, `${STAMP} Pending Counsel`, {
    years: 5, specializations: ['Welfare Law'], languages: ['Tamil'],
    state: 'Tamil Nadu', district: 'Chennai', status: 'PENDING'
  });
  await seedLawyer(suspended.userId, `${STAMP} Suspended Counsel`, {
    years: 20, specializations: ['Welfare Law'], languages: ['Tamil'],
    state: 'Tamil Nadu', district: 'Chennai', status: 'SUSPENDED'
  });

  // --- Who appears -------------------------------------------------------
  console.log('  -- listing --');

  const listing = await request('GET', `/lawyers?q=${STAMP}`, { token: citizen.token });
  const listedNames = (listing.body || []).map((l) => l.fullName);
  check('verified lawyer appears in the directory',
    listedNames.some((n) => n.includes('Verified')));
  check('PENDING lawyer does NOT appear',
    !listedNames.some((n) => n.includes('Pending')));
  check('SUSPENDED lawyer does NOT appear',
    !listedNames.some((n) => n.includes('Suspended')));

  // --- Contact details must never be browsable ---------------------------
  console.log('\n  -- contact privacy --');

  const listingText = JSON.stringify(listing.body);
  check('listing never contains the email', !listingText.includes(SECRET_EMAIL));
  check('listing never contains the phone', !listingText.includes(SECRET_PHONE));
  check('listing has no contactEmail/contactPhone fields at all',
    !/contactEmail|contactPhone|contact_email|contact_phone/i.test(listingText));

  const profile = await request('GET', `/lawyers/${verified.userId}`, { token: citizen.token });
  const profileText = JSON.stringify(profile.body);
  check('profile fetch succeeds', profile.status === 200, `status ${profile.status}`);
  check('profile never contains the email', !profileText.includes(SECRET_EMAIL));
  check('profile never contains the phone', !profileText.includes(SECRET_PHONE));

  const pendingProfile = await request('GET', `/lawyers/${pending.userId}`, { token: citizen.token });
  check('an unverified lawyer profile is 404', pendingProfile.status === 404,
    `status ${pendingProfile.status}`);

  // --- The request / accept cycle ----------------------------------------
  console.log('\n  -- contact request cycle --');

  const reqRes = await request('POST', `/lawyers/${verified.userId}/contact-requests`, {
    token: citizen.token,
    body: { message: 'I would like help with a rejected scheme application.' }
  });
  check('citizen can send a contact request', reqRes.status === 201, `status ${reqRes.status}`);

  const beforeAccept = await request('GET', '/lawyers/contact-requests/mine', { token: citizen.token });
  const beforeText = JSON.stringify(beforeAccept.body);
  check('while PENDING, details are still withheld',
    !beforeText.includes(SECRET_EMAIL) && !beforeText.includes(SECRET_PHONE));
  check('the pending request is visible to its sender',
    (beforeAccept.body || []).some((r) => r.status === 'PENDING'));

  const inbox = await request('GET', '/lawyers/me/inbox', { token: verified.token });
  check('lawyer sees the request in their inbox',
    Array.isArray(inbox.body) && inbox.body.length === 1,
    `got ${Array.isArray(inbox.body) ? inbox.body.length : inbox.status}`);
  const requestId = Array.isArray(inbox.body) && inbox.body[0] ? inbox.body[0].id : null;

  const otherInbox = await request('GET', '/lawyers/me/inbox', { token: citizen.token });
  check('a citizen cannot read a lawyer inbox', otherInbox.status === 403,
    `status ${otherInbox.status}`);

  const accept = await request('POST', `/lawyers/me/inbox/${requestId}/respond`, {
    token: verified.token, body: { action: 'ACCEPTED' }
  });
  check('lawyer can accept', accept.status === 204, `status ${accept.status}`);

  const afterAccept = await request('GET', '/lawyers/contact-requests/mine', { token: citizen.token });
  const afterText = JSON.stringify(afterAccept.body);
  check('after acceptance the email IS released', afterText.includes(SECRET_EMAIL));
  check('after acceptance the phone IS released', afterText.includes(SECRET_PHONE));

  // The crucial negative: acceptance for one citizen must not leak to another.
  const bystander = await request('GET', '/lawyers/contact-requests/mine', { token: citizen2.token });
  check('an unrelated citizen sees none of it',
    !JSON.stringify(bystander.body).includes(SECRET_EMAIL));
  const bystanderListing = await request('GET', `/lawyers?q=${STAMP}`, { token: citizen2.token });
  check('acceptance does not make details public in the directory',
    !JSON.stringify(bystanderListing.body).includes(SECRET_EMAIL));

  // --- Blocking ----------------------------------------------------------
  console.log('\n  -- blocking --');

  const req2 = await request('POST', `/lawyers/${verified.userId}/contact-requests`, {
    token: citizen2.token, body: { message: 'A second citizen asking for contact details.' }
  });
  check('second citizen can send a request', req2.status === 201, `status ${req2.status}`);

  const inbox2 = await request('GET', '/lawyers/me/inbox', { token: verified.token });
  const req2Id = (inbox2.body || []).find((r) => r.status === 'PENDING');
  await request('POST', `/lawyers/me/inbox/${req2Id.id}/respond`, {
    token: verified.token, body: { action: 'BLOCKED' }
  });

  const inboxAfterBlock = await request('GET', '/lawyers/me/inbox', { token: verified.token });
  check('a blocked request disappears from the inbox',
    !(inboxAfterBlock.body || []).some((r) => r.id === req2Id.id));

  const reSend = await request('POST', `/lawyers/${verified.userId}/contact-requests`, {
    token: citizen2.token, body: { message: 'Trying again after being blocked.' }
  });
  check('a blocked sender gets an indistinguishable success', reSend.status === 201,
    `status ${reSend.status}`);
  check('the re-send does not reach the inbox',
    !(await request('GET', '/lawyers/me/inbox', { token: verified.token }))
      .body.some((r) => r.status === 'PENDING'));
  const blockedState = await pool.query(
    'SELECT status FROM contact_requests WHERE citizen_user_id = $1 AND lawyer_user_id = $2',
    [citizen2.userId, verified.userId]
  );
  check('the block survives the re-send in the database',
    blockedState.rows[0] && blockedState.rows[0].status === 'BLOCKED',
    `status ${blockedState.rows[0] && blockedState.rows[0].status}`);
  check('a blocked citizen still never sees contact details',
    !JSON.stringify(
      (await request('GET', '/lawyers/contact-requests/mine', { token: citizen2.token })).body
    ).includes(SECRET_EMAIL));

  // --- Filters -----------------------------------------------------------
  console.log('\n  -- filters --');

  const bySpec = await request('GET', `/lawyers?q=${STAMP}&specialization=Consumer`, { token: citizen.token });
  check('specialization filter matches', (bySpec.body || []).length === 1);
  const byMissingSpec = await request('GET', `/lawyers?q=${STAMP}&specialization=Maritime`, { token: citizen.token });
  check('specialization filter excludes', (byMissingSpec.body || []).length === 0);
  const byLang = await request('GET', `/lawyers?q=${STAMP}&language=Tamil`, { token: citizen.token });
  check('language filter matches', (byLang.body || []).length === 1);
  const byExp = await request('GET', `/lawyers?q=${STAMP}&minExperience=15`, { token: citizen.token });
  check('minExperience excludes a 12-year lawyer', (byExp.body || []).length === 0);
  const byDistrict = await request('GET', `/lawyers?q=${STAMP}&district=Madurai`, { token: citizen.token });
  check('district filter excludes', (byDistrict.body || []).length === 0);

  // --- Availability ------------------------------------------------------
  console.log('\n  -- availability --');

  await request('POST', '/lawyers/me/availability', {
    token: verified.token, body: { acceptingQuestions: false }
  });
  const unavailable = await request('POST', `/lawyers/${verified.userId}/contact-requests`, {
    token: citizen.token, body: { message: 'Should be refused while unavailable.' }
  });
  check('a lawyer not accepting requests refuses new ones', unavailable.status === 409,
    `status ${unavailable.status}`);
  const filtered = await request('GET', `/lawyers?q=${STAMP}&availableOnly=true`, { token: citizen.token });
  check('availableOnly hides them from the directory', (filtered.body || []).length === 0);

  const selfContact = await request('POST', `/lawyers/${verified.userId}/contact-requests`, {
    token: verified.token, body: { message: 'Contacting myself should be refused.' }
  });
  check('a lawyer cannot contact themselves', selfContact.status === 400,
    `status ${selfContact.status}`);

  await cleanup(names);

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
    await cleanup([
      `${STAMP}citizen`, `${STAMP}citizen2`,
      `${STAMP}verified`, `${STAMP}pending`, `${STAMP}suspended`
    ]);
  } catch (_) {}
  try { await pool.end(); } catch (_) {}
  process.exit(1);
});
