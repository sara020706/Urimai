const express = require('express');
const pool = require('../db/pool');
const { authed } = require('../middleware/auth');
const { wrap } = require('../lib/async');

const router = express.Router();

const TARGET_TYPES = new Set(['QUESTION', 'ANSWER', 'LAWYER']);

/**
 * Report abusive or inappropriate content.
 *
 * The unique constraint on (reporter, target_type, target_id) makes repeat
 * reporting idempotent rather than a way to flood the moderation queue.
 */
router.post('/', ...authed, wrap(async (req, res) => {
  const { targetType, targetId, reason, details } = req.body || {};
  const type = String(targetType || '').toUpperCase();

  if (!TARGET_TYPES.has(type) || !targetId) {
    return res.status(400).json({
      error: 'targetType must be QUESTION, ANSWER or LAWYER, and targetId is required.',
      code: 'VALIDATION_FAILED'
    });
  }
  if (!String(reason || '').trim()) {
    return res.status(400).json({ error: 'A reason is required.', code: 'VALIDATION_FAILED' });
  }

  try {
    const result = await pool.query(
      `INSERT INTO content_reports (reporter_user_id, target_type, target_id, reason, details)
       VALUES ($1,$2,$3,$4,$5)
       RETURNING id, status, created_at`,
      [req.userId, type, targetId, String(reason).trim(), details ? String(details).trim() : null]
    );
    const r = result.rows[0];
    res.status(201).json({
      id: String(r.id),
      status: r.status,
      createdAt: new Date(r.created_at).getTime()
    });
  } catch (err) {
    if (err.code === '23505') {
      // Already reported by this user: treat as success so the UI need not
      // special-case it, and so repeat taps do not look like failures.
      return res.status(200).json({ status: 'ALREADY_REPORTED' });
    }
    throw err;
  }
}));

module.exports = router;
