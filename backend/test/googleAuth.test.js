/**
 * Google ID token verification.
 *
 * The entire security of Google sign-in rests on verifyIdToken refusing a
 * token it cannot trace to Google's published keys. If this suite ever fails,
 * anyone can mint a token for any email and take over that account, so these
 * cases are written as attacks rather than as happy-path coverage.
 *
 * Runs offline: every token here is rejected before any network call, so there
 * is no dependency on Google being reachable.
 */

process.env.GOOGLE_CLIENT_ID =
  process.env.GOOGLE_CLIENT_ID || 'test-client-id.apps.googleusercontent.com';

const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const google = require('../src/lib/googleAuth');

let passed = 0;
let failed = 0;

async function mustReject(name, token) {
  try {
    await google.verifyIdToken(token);
    console.log(`  FAIL  ${name} -> ACCEPTED`);
    failed++;
  } catch (err) {
    if (err.code === 'GOOGLE_TOKEN_INVALID' || err.code === 'GOOGLE_NOT_CONFIGURED') {
      console.log(`  PASS  ${name}`);
      passed++;
    } else {
      console.log(`  FAIL  ${name} -> unexpected ${err.code}: ${err.message}`);
      failed++;
    }
  }
}

(async () => {
  console.log('\nGoogle ID token verification\n');

  const { privateKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });

  // Correct issuer, correct audience, verified email -- and signed by a key
  // that is not Google's. This is what a real account-takeover attempt looks
  // like, and the signature check is the only thing standing against it.
  await mustReject(
    'token signed with an attacker key is refused',
    jwt.sign(
      { sub: '123', email: 'victim@example.com', email_verified: true },
      privateKey,
      {
        algorithm: 'RS256',
        issuer: 'https://accounts.google.com',
        audience: process.env.GOOGLE_CLIENT_ID,
        expiresIn: '1h',
        keyid: 'attacker-kid'
      }
    )
  );

  // alg:none -- accepted by any verifier that trusts the header.
  const noneToken =
    Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT', kid: 'x' })).toString('base64url') +
    '.' +
    Buffer.from(JSON.stringify({ sub: '123', email: 'victim@example.com' })).toString('base64url') +
    '.';
  await mustReject('unsigned alg:none token is refused', noneToken);

  // Algorithm downgrade: if the verifier used the header's alg, the public key
  // would be treated as an HMAC secret.
  await mustReject(
    'HS256 downgrade token is refused',
    jwt.sign({ sub: '123' }, 'secret', { algorithm: 'HS256', keyid: 'x' })
  );

  await mustReject('malformed token is refused', 'not-a-token');
  await mustReject('empty token is refused', '');
  await mustReject('missing token is refused', undefined);

  // An expired token signed by an attacker must still fail; expiry is not the
  // only thing being checked.
  await mustReject(
    'expired attacker token is refused',
    jwt.sign({ sub: '123' }, privateKey, {
      algorithm: 'RS256',
      issuer: 'https://accounts.google.com',
      audience: process.env.GOOGLE_CLIENT_ID,
      expiresIn: '-1h',
      keyid: 'attacker-kid'
    })
  );

  console.log(`\n${passed} passed, ${failed} failed\n`);
  process.exit(failed === 0 ? 0 : 1);
})();
