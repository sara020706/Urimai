const express = require('express');
const pool = require('../db/pool');
const { authed, lawyerOnly } = require('../middleware/auth');
const { wrap } = require('../lib/async');
const { contactLimiter } = require('../lib/rateLimit');

const router = express.Router();

/**
 * Verified-lawyer directory and proxied contact.
 *
 * ===================== CONTACT DETAILS ARE NEVER BROWSED =====================
 * No listing or profile response here includes contact_email or contact_phone.
 * A citizen sends a contact request; the lawyer accepts, declines or blocks;
 * details are revealed only on acceptance, only to that citizen, through
 * GET /lawyers/contact-requests/mine.
 *
 * The reason is one-way damage: a phone number published in a searchable
 * directory cannot be unpublished once scraped, and the people exposed are real
 * professionals who did not consent to a public listing of their mobile number.
 * =============================================================================
 */

/** Columns safe to show anyone. Contact fields are deliberately absent. */
const PUBLIC_COLUMNS = `
  lp.user_id, lp.full_name, lp.bar_council_state, lp.enrollment_year,
  lp.years_experience, lp.specializations, lp.languages,
  lp.practice_state, lp.practice_district, lp.bio, lp.accepting_questions
`;

function toApiLawyer(row) {
  return {
    userId: String(row.user_id),
    fullName: row.full_name,
    barCouncilState: row.bar_council_state,
    enrollmentYear: row.enrollment_year,
    yearsExperience: row.years_experience,
    specializations: row.specializations || [],
    languages: row.languages || [],
    practiceState: row.practice_state,
    practiceDistrict: row.practice_district,
    bio: row.bio,
    acceptingQuestions: row.accepting_questions,
    // Verified is the only state that reaches the directory at all, so this is
    // constant by construction; it is returned so the UI can show the badge
    // without inferring it.
    verified: true
  };
}

/**
 * Browse verified lawyers.
 *
 * The WHERE clause pins verification_status and account_status, so an
 * unverified, suspended or blocked lawyer cannot appear no matter what filters
 * are supplied.
 */
router.get('/', ...authed, wrap(async (req, res) => {
  const limit = Math.min(Number(req.query.limit) || 25, 100);
  const offset = Math.max(Number(req.query.offset) || 0, 0);

  const specializations = req.query.specialization
    ? String(req.query.specialization).split(',').map((v) => v.trim()).filter(Boolean)
    : null;
  const languages = req.query.language
    ? String(req.query.language).split(',').map((v) => v.trim()).filter(Boolean)
    : null;

  const result = await pool.query(
    `SELECT ${PUBLIC_COLUMNS}
       FROM lawyer_profiles lp
       JOIN users u ON u.id = lp.user_id
      WHERE lp.verification_status = 'VERIFIED'
        AND u.account_status = 'ACTIVE'
        AND ($1::text IS NULL OR lp.practice_state = $1)
        AND ($2::text IS NULL OR lp.practice_district = $2)
        AND ($3::text[] IS NULL OR lp.specializations && $3)
        AND ($4::text[] IS NULL OR lp.languages && $4)
        AND ($5::int IS NULL OR lp.years_experience >= $5)
        AND ($6::boolean IS NOT TRUE OR lp.accepting_questions = TRUE)
        AND ($7::text IS NULL OR lp.full_name ILIKE '%' || $7 || '%')
      ORDER BY lp.years_experience DESC, lp.full_name ASC
      LIMIT $8 OFFSET $9`,
    [
      req.query.state || null,
      req.query.district || null,
      specializations && specializations.length ? specializations : null,
      languages && languages.length ? languages : null,
      req.query.minExperience ? Number(req.query.minExperience) : null,
      String(req.query.availableOnly || '') === 'true',
      req.query.q || null,
      limit,
      offset
    ]
  );
  res.json(result.rows.map(toApiLawyer));
}));

/**
 * The caller's own contact requests, with details revealed where accepted.
 *
 * Declared before /:id so the literal path is not captured by the parameter.
 */
router.get('/contact-requests/mine', ...authed, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT cr.id, cr.status, cr.message, cr.created_at, cr.responded_at,
            lp.user_id, lp.full_name, lp.practice_state, lp.practice_district,
            lp.years_experience, lp.specializations,
            -- Contact details are released by the CASE, not by the client:
            -- a non-accepted row returns NULL from the database itself.
            CASE WHEN cr.status = 'ACCEPTED' THEN lp.contact_email END AS contact_email,
            CASE WHEN cr.status = 'ACCEPTED' THEN lp.contact_phone END AS contact_phone
       FROM contact_requests cr
       JOIN lawyer_profiles lp ON lp.user_id = cr.lawyer_user_id
      WHERE cr.citizen_user_id = $1
      ORDER BY cr.created_at DESC`,
    [req.userId]
  );

  res.json(result.rows.map((row) => ({
    id: String(row.id),
    status: row.status,
    message: row.message,
    createdAt: new Date(row.created_at).getTime(),
    respondedAt: row.responded_at ? new Date(row.responded_at).getTime() : null,
    lawyer: {
      userId: String(row.user_id),
      fullName: row.full_name,
      practiceState: row.practice_state,
      practiceDistrict: row.practice_district,
      yearsExperience: row.years_experience,
      specializations: row.specializations || []
    },
    // Null unless the lawyer accepted.
    contactEmail: row.contact_email,
    contactPhone: row.contact_phone
  })));
}));

// ---------------------------------------------------------------------------
// Lawyer side of contact handling. Mounted under /lawyers but requires a
// verified lawyer; see server.js for the separate /lawyer routers.
// ---------------------------------------------------------------------------

/**
 * Inbound requests.
 *
 * The citizen's identity is shown here, unlike anonymous Q&A: a citizen who
 * deliberately asks for direct contact is choosing to identify themselves.
 * BLOCKED rows are excluded so a blocked sender disappears from the inbox.
 */
router.get('/me/inbox', ...lawyerOnly, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT cr.id, cr.status, cr.message, cr.created_at, cr.responded_at,
            u.display_name, up.district, up.state
       FROM contact_requests cr
       JOIN users u ON u.id = cr.citizen_user_id
       LEFT JOIN user_profiles up ON up.user_id = cr.citizen_user_id
      WHERE cr.lawyer_user_id = $1
        AND cr.status <> 'BLOCKED'
      ORDER BY cr.created_at DESC`,
    [req.userId]
  );
  res.json(result.rows.map((row) => ({
    id: String(row.id),
    status: row.status,
    message: row.message,
    citizenName: row.display_name,
    citizenDistrict: row.district,
    citizenState: row.state,
    createdAt: new Date(row.created_at).getTime(),
    respondedAt: row.responded_at ? new Date(row.responded_at).getTime() : null
  })));
}));

router.post('/me/inbox/:id/respond', ...lawyerOnly, wrap(async (req, res) => {
  const action = String((req.body || {}).action || '').toUpperCase();
  if (!['ACCEPTED', 'DECLINED', 'BLOCKED'].includes(action)) {
    return res.status(400).json({
      error: 'action must be ACCEPTED, DECLINED or BLOCKED.',
      code: 'VALIDATION_FAILED'
    });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    // Ownership in the WHERE clause, as everywhere else.
    const result = await client.query(
      `UPDATE contact_requests
          SET status = $3, responded_at = now()
        WHERE id = $1 AND lawyer_user_id = $2
        RETURNING id, status, citizen_user_id`,
      [req.params.id, req.userId, action]
    );
    if (result.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Request not found.', code: 'NOT_FOUND' });
    }

    // Only acceptance is announced. A decline or block is not news the sender
    // needs pushed at them, and silence is also what makes a block unprobeable.
    if (action === 'ACCEPTED') {
      await client.query(
        `INSERT INTO notifications (user_id, type, title, body, target_type, target_id)
         VALUES ($1, 'CONTACT_ACCEPTED', 'A lawyer shared their contact details', NULL,
                 'CONTACT_REQUEST', $2)`,
        [result.rows[0].citizen_user_id, result.rows[0].id]
      );
    }

    await client.query('COMMIT');
    res.status(204).send();
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

/** Master switch for a lawyer's own availability. */
router.post('/me/availability', ...lawyerOnly, wrap(async (req, res) => {
  const accepting = (req.body || {}).acceptingQuestions;
  if (typeof accepting !== 'boolean') {
    return res.status(400).json({
      error: 'acceptingQuestions must be true or false.',
      code: 'VALIDATION_FAILED'
    });
  }
  await pool.query(
    'UPDATE lawyer_profiles SET accepting_questions = $2, updated_at = now() WHERE user_id = $1',
    [req.userId, accepting]
  );
  res.json({ acceptingQuestions: accepting });
}));

router.get('/:id', ...authed, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT ${PUBLIC_COLUMNS}
       FROM lawyer_profiles lp
       JOIN users u ON u.id = lp.user_id
      WHERE lp.user_id = $1
        AND lp.verification_status = 'VERIFIED'
        AND u.account_status = 'ACTIVE'`,
    [req.params.id]
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Lawyer not found.', code: 'NOT_FOUND' });
  }
  res.json(toApiLawyer(result.rows[0]));
}));

/**
 * Ask a lawyer for contact details.
 *
 * Rate limited per citizen. A BLOCKED pairing silently returns the same shape
 * as a fresh request so the sender cannot probe whether they were blocked;
 * nothing is written and the lawyer sees nothing.
 */
router.post('/:id/contact-requests', ...authed, contactLimiter, wrap(async (req, res) => {
  const message = String((req.body || {}).message || '').trim();
  if (message.length > 1000) {
    return res.status(400).json({ error: 'Message is too long.', code: 'VALIDATION_FAILED' });
  }
  if (String(req.params.id) === String(req.userId)) {
    return res.status(400).json({ error: 'You cannot contact yourself.', code: 'VALIDATION_FAILED' });
  }

  const target = await pool.query(
    `SELECT lp.user_id, lp.accepting_questions
       FROM lawyer_profiles lp
       JOIN users u ON u.id = lp.user_id
      WHERE lp.user_id = $1
        AND lp.verification_status = 'VERIFIED'
        AND u.account_status = 'ACTIVE'`,
    [req.params.id]
  );
  if (target.rowCount === 0) {
    return res.status(404).json({ error: 'Lawyer not found.', code: 'NOT_FOUND' });
  }
  if (!target.rows[0].accepting_questions) {
    return res.status(409).json({
      error: 'This lawyer is not accepting new requests right now.',
      code: 'NOT_ACCEPTING'
    });
  }

  const existing = await pool.query(
    'SELECT id, status FROM contact_requests WHERE citizen_user_id = $1 AND lawyer_user_id = $2',
    [req.userId, req.params.id]
  );

  if (existing.rowCount > 0) {
    const current = existing.rows[0];
    if (current.status === 'BLOCKED') {
      // Indistinguishable from a successful send. Telling the sender they are
      // blocked invites evasion and retaliation.
      return res.status(201).json({ id: String(current.id), status: 'PENDING' });
    }
    if (current.status === 'PENDING' || current.status === 'ACCEPTED') {
      return res.status(200).json({ id: String(current.id), status: current.status });
    }
    // DECLINED: allow one fresh attempt by reopening the existing row.
    const reopened = await pool.query(
      `UPDATE contact_requests
          SET status = 'PENDING', message = $3, created_at = now(), responded_at = NULL
        WHERE id = $1 AND citizen_user_id = $2
        RETURNING id, status`,
      [current.id, req.userId, message || null]
    );
    return res.status(201).json({
      id: String(reopened.rows[0].id),
      status: reopened.rows[0].status
    });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const inserted = await client.query(
      `INSERT INTO contact_requests (citizen_user_id, lawyer_user_id, message)
       VALUES ($1,$2,$3) RETURNING id, status`,
      [req.userId, req.params.id, message || null]
    );
    await client.query(
      `INSERT INTO notifications (user_id, type, title, body, target_type, target_id)
       VALUES ($1, 'CONTACT_REQUEST', 'A citizen would like to contact you', NULL, 'CONTACT_REQUEST', $2)`,
      [req.params.id, inserted.rows[0].id]
    );
    await client.query('COMMIT');
    res.status(201).json({
      id: String(inserted.rows[0].id),
      status: inserted.rows[0].status
    });
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

module.exports = router;
