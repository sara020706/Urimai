const express = require('express');
const pool = require('../db/pool');
const { adminOnly } = require('../middleware/auth');
const { wrap } = require('../lib/async');
const { audit } = require('../lib/audit');

const router = express.Router();

const ACCOUNT_STATUSES = new Set(['ACTIVE', 'SUSPENDED', 'BLOCKED']);
const VERIFICATION_STATUSES = new Set([
  'PENDING', 'VERIFIED', 'REJECTED', 'SUSPENDED', 'MORE_INFO_REQUESTED'
]);

function toApiUser(row) {
  return {
    id: String(row.id),
    username: row.username,
    displayName: row.display_name,
    role: row.role,
    accountStatus: row.account_status,
    statusReason: row.status_reason,
    createdAt: new Date(row.created_at).getTime(),
    lawyerVerificationStatus: row.lawyer_status || null
  };
}

// ---------------------------------------------------------------------------
// User management
// ---------------------------------------------------------------------------

router.get('/users', ...adminOnly, wrap(async (req, res) => {
  const { role, status, q } = req.query;
  const limit = Math.min(Number(req.query.limit) || 50, 200);
  const offset = Math.max(Number(req.query.offset) || 0, 0);

  const result = await pool.query(
    `SELECT u.id, u.username, u.display_name, u.role, u.account_status,
            u.status_reason, u.created_at, lp.verification_status AS lawyer_status
       FROM users u
       LEFT JOIN lawyer_profiles lp ON lp.user_id = u.id
      WHERE ($1::text IS NULL OR u.role = $1)
        AND ($2::text IS NULL OR u.account_status = $2)
        AND ($3::text IS NULL OR u.username ILIKE '%' || $3 || '%'
                              OR u.display_name ILIKE '%' || $3 || '%')
      ORDER BY u.created_at DESC
      LIMIT $4 OFFSET $5`,
    [role || null, status || null, q || null, limit, offset]
  );
  res.json(result.rows.map(toApiUser));
}));

/**
 * Change an account's status.
 *
 * Blocking or suspending bumps token_valid_from, which invalidates every token
 * already issued to that user. Without this the decision would not take effect
 * until their 30-day token expired.
 */
router.post('/users/:id/status', ...adminOnly, wrap(async (req, res) => {
  const { status, reason } = req.body || {};
  if (!ACCOUNT_STATUSES.has(status)) {
    return res.status(400).json({
      error: 'status must be ACTIVE, SUSPENDED or BLOCKED.',
      code: 'VALIDATION_FAILED'
    });
  }

  // An admin locking themselves out would leave the platform unmanageable.
  if (String(req.params.id) === String(req.userId) && status !== 'ACTIVE') {
    return res.status(400).json({
      error: 'You cannot suspend or block your own account.',
      code: 'SELF_ACTION_FORBIDDEN'
    });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query(
      `UPDATE users
          SET account_status = $1,
              status_reason = $2,
              status_changed_at = now(),
              token_valid_from = CASE WHEN $1 <> 'ACTIVE' THEN now() ELSE token_valid_from END
        WHERE id = $3
        RETURNING id, username, display_name, role, account_status, status_reason, created_at`,
      [status, reason || null, req.params.id]
    );

    if (result.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'User not found.', code: 'NOT_FOUND' });
    }

    await audit(req, {
      action: 'USER_STATUS_CHANGE',
      targetType: 'USER',
      targetId: req.params.id,
      reason,
      metadata: { status }
    }, client);

    await client.query('COMMIT');
    res.json(toApiUser(result.rows[0]));
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

// ---------------------------------------------------------------------------
// Lawyer verification
// ---------------------------------------------------------------------------

router.get('/lawyers', ...adminOnly, wrap(async (req, res) => {
  const status = req.query.status || null;
  const result = await pool.query(
    `SELECT lp.*, u.username, u.display_name, u.account_status,
            (SELECT count(*) FROM lawyer_verification_documents d
              WHERE d.lawyer_user_id = lp.user_id)::int AS document_count
       FROM lawyer_profiles lp
       JOIN users u ON u.id = lp.user_id
      WHERE ($1::text IS NULL OR lp.verification_status = $1)
      ORDER BY
        CASE lp.verification_status
          WHEN 'PENDING' THEN 0
          WHEN 'MORE_INFO_REQUESTED' THEN 1
          ELSE 2 END,
        lp.created_at ASC`,
    [status]
  );

  res.json(result.rows.map((row) => ({
    userId: String(row.user_id),
    username: row.username,
    displayName: row.display_name,
    accountStatus: row.account_status,
    fullName: row.full_name,
    barCouncilRegNumber: row.bar_council_reg_number,
    barCouncilState: row.bar_council_state,
    enrollmentYear: row.enrollment_year,
    yearsExperience: row.years_experience,
    specializations: row.specializations || [],
    languages: row.languages || [],
    practiceState: row.practice_state,
    practiceDistrict: row.practice_district,
    bio: row.bio,
    verificationStatus: row.verification_status,
    verificationNotes: row.verification_notes,
    documentCount: row.document_count,
    createdAt: new Date(row.created_at).getTime()
  })));
}));

router.get('/lawyers/:id/documents', ...adminOnly, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT id, doc_type, file_name, mime_type, byte_size, uploaded_at
       FROM lawyer_verification_documents
      WHERE lawyer_user_id = $1
      ORDER BY uploaded_at DESC`,
    [req.params.id]
  );
  res.json(result.rows.map((row) => ({
    id: String(row.id),
    docType: row.doc_type,
    fileName: row.file_name,
    mimeType: row.mime_type,
    byteSize: row.byte_size,
    uploadedAt: new Date(row.uploaded_at).getTime()
  })));
}));

/**
 * Stream one verification document for review.
 *
 * Audited: reading someone's identity document is exactly the kind of
 * privileged access that should leave a trace.
 */
router.get('/lawyers/:id/documents/:docId/file', ...adminOnly, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT file_name, mime_type, file_data
       FROM lawyer_verification_documents
      WHERE id = $1 AND lawyer_user_id = $2`,
    [req.params.docId, req.params.id]
  );
  const doc = result.rows[0];
  if (!doc) {
    return res.status(404).json({ error: 'Document not found.', code: 'NOT_FOUND' });
  }

  await audit(req, {
    action: 'LAWYER_DOCUMENT_VIEW',
    targetType: 'LAWYER',
    targetId: req.params.id,
    metadata: { documentId: req.params.docId }
  });

  res.setHeader('Content-Type', doc.mime_type || 'application/octet-stream');
  // attachment, not inline: the filename is user-supplied, and inline rendering
  // of attacker-controlled content in an admin's browser is an avoidable risk.
  res.setHeader('Content-Disposition', 'attachment');
  res.send(doc.file_data);
}));

router.post('/lawyers/:id/verification', ...adminOnly, wrap(async (req, res) => {
  const { status, notes } = req.body || {};
  if (!VERIFICATION_STATUSES.has(status)) {
    return res.status(400).json({
      error: 'status must be one of PENDING, VERIFIED, REJECTED, SUSPENDED, MORE_INFO_REQUESTED.',
      code: 'VALIDATION_FAILED'
    });
  }
  if ((status === 'REJECTED' || status === 'MORE_INFO_REQUESTED') && !String(notes || '').trim()) {
    return res.status(400).json({
      error: 'Notes are required when rejecting or requesting more information.',
      code: 'NOTES_REQUIRED'
    });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query(
      `UPDATE lawyer_profiles
          SET verification_status = $1,
              verification_notes = $2,
              reviewed_by = $3,
              reviewed_at = now(),
              updated_at = now()
        WHERE user_id = $4
        RETURNING *`,
      [status, notes || null, req.userId, req.params.id]
    );

    if (result.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Lawyer profile not found.', code: 'NOT_FOUND' });
    }

    // Written in the same transaction as the decision, so a lawyer is never
    // approved without being told.
    await client.query(
      `INSERT INTO notifications (user_id, type, title, body, target_type, target_id)
       VALUES ($1, 'VERIFICATION_UPDATE', $2, $3, 'LAWYER', $1)`,
      [
        req.params.id,
        `Verification ${status.toLowerCase().replace(/_/g, ' ')}`,
        notes || null
      ]
    );

    await audit(req, {
      action: 'LAWYER_VERIFICATION_DECISION',
      targetType: 'LAWYER',
      targetId: req.params.id,
      reason: notes,
      metadata: { status }
    }, client);

    await client.query('COMMIT');
    res.json({
      userId: String(result.rows[0].user_id),
      verificationStatus: result.rows[0].verification_status,
      verificationNotes: result.rows[0].verification_notes,
      reviewedAt: new Date(result.rows[0].reviewed_at).getTime()
    });
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

// ---------------------------------------------------------------------------
// Moderation
//
// This is the one place with a broad view of legal_answers. It is role-gated,
// audited, and deliberately separate from every lawyer-facing route, so the
// isolation invariant elsewhere is never weakened to accommodate it.
// ---------------------------------------------------------------------------

router.get('/reports', ...adminOnly, wrap(async (req, res) => {
  const status = req.query.status || 'OPEN';
  const result = await pool.query(
    `SELECT r.*, u.username AS reporter_username
       FROM content_reports r
       JOIN users u ON u.id = r.reporter_user_id
      WHERE ($1::text IS NULL OR r.status = $1)
      ORDER BY r.created_at DESC
      LIMIT 100`,
    [status === 'ALL' ? null : status]
  );
  res.json(result.rows.map((row) => ({
    id: String(row.id),
    reporterUsername: row.reporter_username,
    targetType: row.target_type,
    targetId: String(row.target_id),
    reason: row.reason,
    details: row.details,
    status: row.status,
    resolutionNote: row.resolution_note,
    createdAt: new Date(row.created_at).getTime()
  })));
}));

/** The reported content itself, so an admin can judge it rather than guess. */
router.get('/reports/:id/content', ...adminOnly, wrap(async (req, res) => {
  const report = await pool.query('SELECT target_type, target_id FROM content_reports WHERE id = $1', [req.params.id]);
  if (report.rowCount === 0) {
    return res.status(404).json({ error: 'Report not found.', code: 'NOT_FOUND' });
  }
  const { target_type: type, target_id: id } = report.rows[0];

  if (type === 'QUESTION') {
    const q = await pool.query(
      'SELECT id, title, body, status, is_hidden FROM legal_questions WHERE id = $1', [id]
    );
    return res.json({ targetType: type, content: q.rows[0] || null });
  }
  if (type === 'ANSWER') {
    const a = await pool.query(
      `SELECT a.id, a.body, a.is_hidden, a.lawyer_user_id, u.username AS lawyer_username
         FROM legal_answers a JOIN users u ON u.id = a.lawyer_user_id
        WHERE a.id = $1`, [id]
    );
    await audit(req, { action: 'MODERATION_VIEW_ANSWER', targetType: 'ANSWER', targetId: id });
    return res.json({ targetType: type, content: a.rows[0] || null });
  }
  const l = await pool.query(
    `SELECT lp.user_id, lp.full_name, lp.bio, lp.verification_status
       FROM lawyer_profiles lp WHERE lp.user_id = $1`, [id]
  );
  res.json({ targetType: type, content: l.rows[0] || null });
}));

router.post('/reports/:id/resolve', ...adminOnly, wrap(async (req, res) => {
  const { action, note } = req.body || {};
  if (!['HIDE', 'UNHIDE', 'DISMISS'].includes(action)) {
    return res.status(400).json({
      error: 'action must be HIDE, UNHIDE or DISMISS.',
      code: 'VALIDATION_FAILED'
    });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const report = await client.query(
      'SELECT target_type, target_id FROM content_reports WHERE id = $1 FOR UPDATE',
      [req.params.id]
    );
    if (report.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Report not found.', code: 'NOT_FOUND' });
    }

    const { target_type: type, target_id: targetId } = report.rows[0];
    const hide = action === 'HIDE';

    if (action !== 'DISMISS') {
      if (type === 'QUESTION') {
        await client.query(
          `UPDATE legal_questions
              SET is_hidden = $2, hidden_reason = $3,
                  status = CASE WHEN $2 THEN 'REMOVED'
                                WHEN status = 'REMOVED' THEN 'OPEN' ELSE status END,
                  updated_at = now()
            WHERE id = $1`,
          [targetId, hide, hide ? (note || 'Removed after moderation.') : null]
        );
      } else if (type === 'ANSWER') {
        await client.query(
          `UPDATE legal_answers SET is_hidden = $2, hidden_reason = $3, updated_at = now()
            WHERE id = $1`,
          [targetId, hide, hide ? (note || 'Removed after moderation.') : null]
        );
      }
    }

    await client.query(
      `UPDATE content_reports
          SET status = $2, resolved_by = $3, resolved_at = now(), resolution_note = $4
        WHERE id = $1`,
      [req.params.id, action === 'DISMISS' ? 'DISMISSED' : 'RESOLVED', req.userId, note || null]
    );

    await audit(req, {
      action: 'MODERATION_RESOLVE',
      targetType: type,
      targetId,
      reason: note,
      metadata: { action, reportId: req.params.id }
    }, client);

    await client.query('COMMIT');
    res.status(204).send();
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

// ---------------------------------------------------------------------------
// Audit log
// ---------------------------------------------------------------------------

router.get('/audit-log', ...adminOnly, wrap(async (req, res) => {
  const limit = Math.min(Number(req.query.limit) || 50, 200);
  const offset = Math.max(Number(req.query.offset) || 0, 0);
  const result = await pool.query(
    `SELECT a.*, u.username AS admin_username
       FROM admin_audit_log a
       JOIN users u ON u.id = a.admin_user_id
      WHERE ($1::text IS NULL OR a.target_type = $1)
      ORDER BY a.created_at DESC
      LIMIT $2 OFFSET $3`,
    [req.query.targetType || null, limit, offset]
  );
  res.json(result.rows.map((row) => ({
    id: String(row.id),
    adminUsername: row.admin_username,
    action: row.action,
    targetType: row.target_type,
    targetId: row.target_id,
    reason: row.reason,
    metadata: row.metadata,
    createdAt: new Date(row.created_at).getTime()
  })));
}));

module.exports = router;
