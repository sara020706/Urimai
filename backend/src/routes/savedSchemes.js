const express = require('express');
const pool = require('../db/pool');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

router.get('/', requireAuth, async (req, res) => {
  const result = await pool.query('SELECT scheme_id FROM saved_schemes WHERE user_id = $1', [req.userId]);
  res.json(result.rows.map((r) => r.scheme_id));
});

router.put('/:schemeId', requireAuth, async (req, res) => {
  await pool.query(
    `INSERT INTO saved_schemes (user_id, scheme_id) VALUES ($1, $2)
     ON CONFLICT (user_id, scheme_id) DO NOTHING`,
    [req.userId, req.params.schemeId]
  );
  res.status(204).send();
});

router.delete('/:schemeId', requireAuth, async (req, res) => {
  await pool.query('DELETE FROM saved_schemes WHERE user_id = $1 AND scheme_id = $2', [req.userId, req.params.schemeId]);
  res.status(204).send();
});

module.exports = router;
