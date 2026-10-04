const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db/pool');
const { wrap } = require('../lib/async');
const { authLimiter } = require('../lib/rateLimit');
const google = require('../lib/googleAuth');

const router = express.Router();

function issueToken(userId) {
  return jwt.sign({ userId }, process.env.JWT_SECRET, { expiresIn: '30d' });
}

router.post('/signup', authLimiter, wrap(async (req, res) => {
  const { username, password, displayName, role } = req.body || {};
  const normalizedUsername = String(username || '').trim().toLowerCase();

  // A client may self-assert LAWYER, never ADMIN. Self-asserting LAWYER is safe
  // only because the role grants nothing until an admin verifies it — which is
  // why capability routes gate on requireVerifiedLawyer, never requireRole.
  const requestedRole = String(role || 'USER').toUpperCase();
  const resolvedRole = requestedRole === 'LAWYER' ? 'LAWYER' : 'USER';

  if (!normalizedUsername || !password) {
    return res.status(400).json({ error: 'Username and password are required.' });
  }
  if (String(password).length < 4) {
    return res.status(400).json({ error: 'Password must be at least 4 characters.' });
  }

  const existing = await pool.query('SELECT id FROM users WHERE username = $1', [normalizedUsername]);
  if (existing.rowCount > 0) {
    return res.status(409).json({ error: 'An account with this username already exists.' });
  }

  const passwordHash = await bcrypt.hash(String(password), 10);
  const resolvedDisplayName = (displayName && String(displayName).trim()) || username;

  const inserted = await pool.query(
    `INSERT INTO users (username, password_hash, display_name, role)
     VALUES ($1, $2, $3, $4) RETURNING id, display_name, role`,
    [normalizedUsername, passwordHash, resolvedDisplayName, resolvedRole]
  );
  const user = inserted.rows[0];

  await pool.query(
    `INSERT INTO user_profiles (user_id, name) VALUES ($1, $2)
     ON CONFLICT (user_id) DO NOTHING`,
    [user.id, resolvedDisplayName]
  );

  res.status(201).json({
    userId: user.id,
    displayName: user.display_name,
    role: user.role,
    token: issueToken(user.id)
  });
}));

router.post('/login', authLimiter, wrap(async (req, res) => {
  const { username, password } = req.body || {};
  const normalizedUsername = String(username || '').trim().toLowerCase();

  if (!normalizedUsername || !password) {
    return res.status(400).json({ error: 'Username and password are required.' });
  }

  const result = await pool.query(
    `SELECT u.id, u.password_hash, u.display_name, u.role, u.account_status,
            u.status_reason, lp.verification_status AS lawyer_status
       FROM users u
       LEFT JOIN lawyer_profiles lp ON lp.user_id = u.id
      WHERE u.username = $1`,
    [normalizedUsername]
  );
  const user = result.rows[0];
  if (!user) {
    return res.status(401).json({ error: 'No account found for this username.' });
  }

  const passwordMatches = await bcrypt.compare(String(password), user.password_hash);
  if (!passwordMatches) {
    return res.status(401).json({ error: 'Incorrect password.' });
  }

  // Refuse a session to an account that cannot use one, rather than issuing a
  // token every request will then reject.
  if (user.account_status !== 'ACTIVE') {
    return res.status(403).json({
      error: user.status_reason ||
        (user.account_status === 'BLOCKED' ? 'This account has been blocked.' : 'This account is suspended.'),
      code: user.account_status === 'BLOCKED' ? 'ACCOUNT_BLOCKED' : 'ACCOUNT_SUSPENDED'
    });
  }

  res.json({
    userId: user.id,
    displayName: user.display_name,
    role: user.role,
    lawyerVerificationStatus: user.lawyer_status || null,
    token: issueToken(user.id)
  });
}));

/**
 * Sign in (or register) with a Google ID token.
 *
 * The client proves its identity with a token Google signed; we verify that
 * signature and only then issue our own JWT. See lib/googleAuth.js for what is
 * checked. The client's own claim about who it is counts for nothing here.
 */
/**
 * What sign-in methods this server actually supports.
 *
 * Unauthenticated by design: the login screen has no token yet, and it needs
 * this to decide whether to show the Google button. Showing a button that
 * always fails is worse than not showing it. Exposes a single boolean and no
 * configuration detail.
 */
router.get('/methods', (_req, res) => {
  res.json({ password: true, google: google.isConfigured() });
});

router.post('/google', authLimiter, wrap(async (req, res) => {
  const { idToken, role } = req.body || {};

  if (!google.isConfigured()) {
    return res.status(503).json({
      error: 'Google sign-in is not available right now.',
      code: 'GOOGLE_NOT_CONFIGURED'
    });
  }

  let identity;
  try {
    identity = await google.verifyIdToken(idToken);
  } catch (err) {
    if (err.code === 'GOOGLE_NOT_CONFIGURED') {
      return res.status(503).json({
        error: 'Google sign-in is not available right now.',
        code: 'GOOGLE_NOT_CONFIGURED'
      });
    }
    return res.status(401).json({ error: err.message, code: 'GOOGLE_TOKEN_INVALID' });
  }

  // Same rule as password signup: a client may self-assert LAWYER, never ADMIN.
  const requestedRole = String(role || 'USER').toUpperCase();
  const resolvedRole = requestedRole === 'LAWYER' ? 'LAWYER' : 'USER';

  // 1. Match on the Google subject id first. It is stable across email changes.
  let result = await pool.query(
    `SELECT u.id, u.display_name, u.role, u.account_status, u.status_reason,
            lp.verification_status AS lawyer_status
       FROM users u
       LEFT JOIN lawyer_profiles lp ON lp.user_id = u.id
      WHERE u.google_sub = $1`,
    [identity.sub]
  );
  let user = result.rows[0];

  // 2. No Google-linked row: this identity is new to us. If the verified email
  //    matches an existing password account, link them rather than creating a
  //    duplicate the citizen cannot tell apart.
  //
  //    Gated on emailVerified: linking on an unverified address would let
  //    someone register a Google account claiming an email they do not own and
  //    inherit the matching Urimai account.
  if (!user && identity.email && identity.emailVerified) {
    const byEmail = await pool.query(
      `SELECT u.id, u.display_name, u.role, u.account_status, u.status_reason,
              lp.verification_status AS lawyer_status
         FROM users u
         LEFT JOIN lawyer_profiles lp ON lp.user_id = u.id
        WHERE u.google_sub IS NULL
          AND (lower(u.email) = $1 OR u.username = $1)`,
      [identity.email]
    );
    if (byEmail.rowCount > 0) {
      const existing = byEmail.rows[0];
      // Claim it only if still unclaimed, so two concurrent logins cannot both
      // link the same row to different Google identities.
      const linked = await pool.query(
        `UPDATE users
            SET google_sub = $1,
                email = COALESCE(email, $2),
                email_verified = TRUE,
                auth_provider = CASE WHEN password_hash IS NULL THEN 'GOOGLE' ELSE 'BOTH' END
          WHERE id = $3 AND google_sub IS NULL
          RETURNING id`,
        [identity.sub, identity.email, existing.id]
      );
      if (linked.rowCount === 1) user = existing;
    }
  }

  // 3. Still nothing: create the account.
  if (!user) {
    if (!identity.email || !identity.emailVerified) {
      return res.status(401).json({
        error: 'Your Google account does not have a verified email address.',
        code: 'GOOGLE_EMAIL_UNVERIFIED'
      });
    }

    const displayName = identity.name || identity.email.split('@')[0];

    // username carries a UNIQUE constraint and is what password login reads.
    // Using the email keeps one human to one row across both methods.
    const inserted = await pool.query(
      `INSERT INTO users (username, password_hash, display_name, role,
                          google_sub, email, email_verified, auth_provider)
       VALUES ($1, NULL, $2, $3, $4, $5, TRUE, 'GOOGLE')
       ON CONFLICT (username) DO NOTHING
       RETURNING id, display_name, role, account_status, status_reason`,
      [identity.email, displayName, resolvedRole, identity.sub, identity.email]
    );

    if (inserted.rowCount === 0) {
      // Lost a race against a concurrent signup for the same address.
      const reread = await pool.query(
        `SELECT u.id, u.display_name, u.role, u.account_status, u.status_reason,
                lp.verification_status AS lawyer_status
           FROM users u
           LEFT JOIN lawyer_profiles lp ON lp.user_id = u.id
          WHERE u.username = $1`,
        [identity.email]
      );
      user = reread.rows[0];
      if (!user) {
        return res.status(409).json({
          error: 'An account with this email already exists.',
          code: 'ACCOUNT_CONFLICT'
        });
      }
    } else {
      user = inserted.rows[0];
      user.lawyer_status = null;
      await pool.query(
        `INSERT INTO user_profiles (user_id, name) VALUES ($1, $2)
         ON CONFLICT (user_id) DO NOTHING`,
        [user.id, displayName]
      );
    }
  }

  // Same refusal as password login: do not hand a session to an account that
  // cannot use one.
  if (user.account_status && user.account_status !== 'ACTIVE') {
    return res.status(403).json({
      error: user.status_reason ||
        (user.account_status === 'BLOCKED' ? 'This account has been blocked.' : 'This account is suspended.'),
      code: user.account_status === 'BLOCKED' ? 'ACCOUNT_BLOCKED' : 'ACCOUNT_SUSPENDED'
    });
  }

  res.json({
    userId: user.id,
    displayName: user.display_name,
    role: user.role,
    lawyerVerificationStatus: user.lawyer_status || null,
    token: issueToken(user.id)
  });
}));

module.exports = router;
