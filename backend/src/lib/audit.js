const pool = require('../db/pool');

/**
 * Append an entry to admin_audit_log.
 *
 * Pass `client` to enlist in a caller's transaction so the audit entry commits
 * atomically with the change it records; omit it for standalone writes.
 */
async function audit(req, { action, targetType, targetId, reason, metadata }, client) {
  const executor = client || pool;
  await executor.query(
    `INSERT INTO admin_audit_log
       (admin_user_id, action, target_type, target_id, reason, metadata, ip_address)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [
      req.userId,
      action,
      targetType,
      targetId != null ? String(targetId) : null,
      reason || null,
      metadata ? JSON.stringify(metadata) : '{}',
      req.ip || null
    ]
  );
}

module.exports = { audit };
