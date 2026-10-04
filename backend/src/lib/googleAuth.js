/**
 * Google ID token verification.
 *
 * The client sends an ID token; we verify it here and issue our own JWT. The
 * client never asserts who it is. Trusting a client-supplied email would mean
 * anyone could POST {email: "<an admin's address>"} and take over that account,
 * so the signature check below is the entire security boundary of Google login.
 *
 * Verified, in order:
 *   1. RS256 signature against Google's published public keys (JWKS)
 *   2. issuer is Google
 *   3. audience is OUR client id -- without this, a token minted for any other
 *      Google app would be accepted here, which is the classic confused-deputy
 *      bug in "sign in with Google" integrations
 *   4. not expired
 *   5. the email is actually verified by Google
 *
 * No new dependency: jsonwebtoken is already present, and Node's crypto can
 * build a public key straight from a JWK.
 */

const crypto = require('crypto');
const jwt = require('jsonwebtoken');

const JWKS_URL = 'https://www.googleapis.com/oauth2/v3/certs';
const GOOGLE_ISSUERS = ['https://accounts.google.com', 'accounts.google.com'];
const JWKS_TIMEOUT_MS = Number(process.env.GOOGLE_JWKS_TIMEOUT_MS || 12000);
// A cold JWKS fetch is the one network call standing between a user and a
// successful login, and connect timeouts to googleapis.com do happen. One
// retry costs nothing on the happy path and avoids failing a sign-in over a
// single blip when no key is cached yet.
const JWKS_ATTEMPTS = Number(process.env.GOOGLE_JWKS_ATTEMPTS || 2);

class GoogleNotConfigured extends Error {
  constructor() {
    super('GOOGLE_CLIENT_ID is not set.');
    this.code = 'GOOGLE_NOT_CONFIGURED';
  }
}

class GoogleTokenInvalid extends Error {
  constructor(message) {
    super(message);
    this.code = 'GOOGLE_TOKEN_INVALID';
  }
}

/**
 * Every client id permitted to mint tokens for us.
 *
 * Android and web clients of the same project have different ids, and an
 * Android app using the standard flow receives a token audienced to the WEB
 * client id. Supporting a list avoids the common failure where login works on
 * web and mysteriously 401s on the phone.
 */
function allowedAudiences() {
  return String(process.env.GOOGLE_CLIENT_ID || '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

function isConfigured() {
  return allowedAudiences().length > 0;
}

// Google rotates these keys but publishes cache headers measured in hours.
// Refetching per login would add a round trip to every sign-in and risk
// rate limiting; caching per key id with a TTL keeps it to roughly one fetch.
let jwksCache = { keys: null, fetchedAt: 0 };
const JWKS_TTL_MS = 60 * 60 * 1000;

async function fetchJwks(force) {
  const fresh = jwksCache.keys && Date.now() - jwksCache.fetchedAt < JWKS_TTL_MS;
  if (fresh && !force) return jwksCache.keys;

  let lastError;
  for (let attempt = 0; attempt < JWKS_ATTEMPTS; attempt++) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), JWKS_TIMEOUT_MS);
    try {
      const response = await fetch(JWKS_URL, { signal: controller.signal });
      if (!response.ok) throw new Error(`JWKS HTTP ${response.status}`);
      const body = await response.json();
      if (!body || !Array.isArray(body.keys)) throw new Error('JWKS payload malformed');
      jwksCache = { keys: body.keys, fetchedAt: Date.now() };
      return body.keys;
    } catch (err) {
      lastError = err;
    } finally {
      clearTimeout(timer);
    }
  }

  // A stale key still verifies tokens signed before the rotation, so serving
  // it beats failing every login while Google is briefly unreachable.
  if (jwksCache.keys) return jwksCache.keys;
  throw new GoogleTokenInvalid('Could not reach Google to verify the sign-in.');
}

async function publicKeyForKid(kid) {
  let keys = await fetchJwks(false);
  let jwk = keys.find((k) => k.kid === kid);

  // An unknown kid usually means Google rotated keys since our last fetch.
  if (!jwk) {
    keys = await fetchJwks(true);
    jwk = keys.find((k) => k.kid === kid);
  }
  if (!jwk) throw new GoogleTokenInvalid('Sign-in token used an unknown signing key.');
  return crypto.createPublicKey({ key: jwk, format: 'jwk' });
}

/**
 * Verify an ID token and return the identity it proves.
 *
 * Throws GoogleTokenInvalid for anything untrustworthy. Callers must not fall
 * back to reading the token's claims when this throws.
 */
async function verifyIdToken(idToken) {
  if (!isConfigured()) throw new GoogleNotConfigured();

  const raw = String(idToken || '').trim();
  if (!raw) throw new GoogleTokenInvalid('No sign-in token was provided.');

  const decoded = jwt.decode(raw, { complete: true });
  if (!decoded || !decoded.header || !decoded.header.kid) {
    throw new GoogleTokenInvalid('Sign-in token is malformed.');
  }
  // Pinned: without this, a token with {"alg":"none"} or an HMAC algorithm
  // could be presented, and a verifier that trusts the header would accept it.
  if (decoded.header.alg !== 'RS256') {
    throw new GoogleTokenInvalid('Sign-in token uses an unexpected algorithm.');
  }

  const key = await publicKeyForKid(decoded.header.kid);

  let claims;
  try {
    claims = jwt.verify(raw, key, {
      algorithms: ['RS256'],
      audience: allowedAudiences(),
      issuer: GOOGLE_ISSUERS
    });
  } catch (err) {
    if (err.name === 'TokenExpiredError') {
      throw new GoogleTokenInvalid('That sign-in has expired. Please try again.');
    }
    throw new GoogleTokenInvalid('Sign-in token could not be verified.');
  }

  if (!claims.sub) throw new GoogleTokenInvalid('Sign-in token carries no account id.');

  // An unverified email must never be used to match an existing account: it
  // would let someone claim an address they do not control.
  const emailVerified = claims.email_verified === true || claims.email_verified === 'true';

  return {
    sub: String(claims.sub),
    email: claims.email ? String(claims.email).trim().toLowerCase() : null,
    emailVerified,
    name: claims.name ? String(claims.name).trim() : null
  };
}

module.exports = {
  isConfigured,
  verifyIdToken,
  GoogleNotConfigured,
  GoogleTokenInvalid
};
