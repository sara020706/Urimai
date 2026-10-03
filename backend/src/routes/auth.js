const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db/pool');
const { wrap } = require('../lib/async');
const { authLimiter } = require('../lib/rateLimit');

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

module.exports = router;
