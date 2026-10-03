const express = require('express');
const pool = require('../db/pool');
const { authed } = require('../middleware/auth');
const { wrap } = require('../lib/async');

const router = express.Router();

function toApiNotification(row) {
  return {
    id: String(row.id),
    type: row.type,
    title: row.title,
    body: row.body,
    targetType: row.target_type,
    targetId: row.target_id != null ? String(row.target_id) : null,
    readAt: row.read_at ? new Date(row.read_at).getTime() : null,
    createdAt: new Date(row.created_at).getTime()
  };
}

router.get('/', ...authed, wrap(async (req, res) => {
  const limit = Math.min(Number(req.query.limit) || 50, 100);
  const unreadOnly = String(req.query.unreadOnly || '') === 'true';

  const result = await pool.query(
    `SELECT * FROM notifications
      WHERE user_id = $1 AND ($2::boolean IS NOT TRUE OR read_at IS NULL)
      ORDER BY created_at DESC
      LIMIT $3`,
    [req.userId, unreadOnly, limit]
  );
  res.json(result.rows.map(toApiNotification));
}));

/**
 * Cheap badge poll, backed by the partial index on (user_id) WHERE read_at IS
 * NULL. This is what makes polling viable in place of push.
 */
router.get('/unread-count', ...authed, wrap(async (req, res) => {
  const result = await pool.query(
    'SELECT count(*)::int AS count FROM notifications WHERE user_id = $1 AND read_at IS NULL',
    [req.userId]
  );
  res.json({ count: result.rows[0].count });
}));

/**
 * Mark notifications read. Omitting `ids` marks all of them.
 *
 * Ownership is in the WHERE clause, so passing another user's notification id
 * matches zero rows rather than updating it.
 */
router.post('/read', ...authed, wrap(async (req, res) => {
  const ids = (req.body || {}).ids;

  if (Array.isArray(ids) && ids.length > 0) {
    await pool.query(
      `UPDATE notifications SET read_at = now()
        WHERE user_id = $1 AND id = ANY($2::bigint[]) AND read_at IS NULL`,
      [req.userId, ids.map((i) => String(i))]
    );
  } else {
    await pool.query(
      'UPDATE notifications SET read_at = now() WHERE user_id = $1 AND read_at IS NULL',
      [req.userId]
    );
  }
  res.status(204).send();
}));

module.exports = router;
