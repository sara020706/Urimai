const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db/pool');

const router = express.Router();

function issueToken(userId) {
  return jwt.sign({ userId }, process.env.JWT_SECRET, { expiresIn: '30d' });
}

router.post('/signup', async (req, res) => {
  const { username, password, displayName } = req.body || {};
  const normalizedUsername = String(username || '').trim().toLowerCase();

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
    `INSERT INTO users (username, password_hash, display_name)
     VALUES ($1, $2, $3) RETURNING id, display_name`,
    [normalizedUsername, passwordHash, resolvedDisplayName]
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
    token: issueToken(user.id)
  });
});

router.post('/login', async (req, res) => {
  const { username, password } = req.body || {};
  const normalizedUsername = String(username || '').trim().toLowerCase();

  if (!normalizedUsername || !password) {
    return res.status(400).json({ error: 'Username and password are required.' });
  }

  const result = await pool.query(
    'SELECT id, password_hash, display_name FROM users WHERE username = $1',
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

  res.json({
    userId: user.id,
    displayName: user.display_name,
    token: issueToken(user.id)
  });
});

module.exports = router;
