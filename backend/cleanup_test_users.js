// Removes throwaway accounts created during manual endpoint testing.
require('dotenv').config();
const pool = require('./src/db/pool');

(async () => {
  const { rows } = await pool.query(
    `SELECT id, username FROM users
      WHERE username ~ '^(law|law2|admin|evil|p0test|ai|iso|dir|mod|dbg)[0-9]{6,}'
         OR username ~ '^(iso|dir|mod|dbg)[0-9]+(citizen|citizen2|verified|pending|suspended|lawa|lawb|c|l|a|b)$'`
  );
  if (rows.length === 0) {
    console.log('no test users found');
  } else {
    // admin_audit_log uses ON DELETE RESTRICT, so its rows go first.
    const ids = rows.map((r) => r.id);
    await pool.query('DELETE FROM admin_audit_log WHERE admin_user_id = ANY($1)', [ids]);
    const del = await pool.query('DELETE FROM users WHERE id = ANY($1) RETURNING username', [ids]);
    console.log('deleted:', del.rows.map((r) => r.username).join(', '));
  }
  const c = await pool.query('SELECT count(*)::int AS n FROM users');
  console.log('remaining users:', c.rows[0].n);
  await pool.end();
})().catch((e) => { console.error(e.message); process.exit(1); });
