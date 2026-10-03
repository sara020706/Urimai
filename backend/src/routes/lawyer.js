const express = require('express');
const multer = require('multer');
const pool = require('../db/pool');
const { authed, requireRole } = require('../middleware/auth');
const { wrap } = require('../lib/async');

const router = express.Router();

// Same 10 MB cap as citizen document upload, with a MIME allowlist: these are
// scanned ID and bar-council documents, so there is no reason to accept
// anything but images and PDFs.
const ALLOWED_MIME = new Set([
  'image/jpeg', 'image/png', 'image/webp', 'application/pdf'
]);

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    if (ALLOWED_MIME.has(file.mimetype)) return cb(null, true);
    const err = new Error('Unsupported file type.');
    err.code = 'UNSUPPORTED_FILE_TYPE';
    cb(err);
  }
});

// Only a LAWYER may manage their own professional profile. Note this uses
// requireRole, not requireVerifiedLawyer: an unverified lawyer must be able to
// submit their details — that is how they become verified.
const lawyerSelf = [...authed, requireRole('LAWYER')];

function toApiLawyerProfile(row) {
  return {
    userId: String(row.user_id),
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
    acceptingQuestions: row.accepting_questions,
    contactEmail: row.contact_email,
    contactPhone: row.contact_phone,
    reviewedAt: row.reviewed_at ? new Date(row.reviewed_at).getTime() : null
  };
}

function toApiVerificationDocument(row) {
  return {
    id: String(row.id),
    docType: row.doc_type,
    fileName: row.file_name,
    mimeType: row.mime_type,
    byteSize: row.byte_size,
    uploadedAt: new Date(row.uploaded_at).getTime()
  };
}

function asStringArray(value) {
  if (!Array.isArray(value)) return [];
  return value.map((v) => String(v).trim()).filter(Boolean);
}

router.get('/me', ...lawyerSelf, wrap(async (req, res) => {
  const result = await pool.query('SELECT * FROM lawyer_profiles WHERE user_id = $1', [req.userId]);
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'No lawyer profile yet.', code: 'PROFILE_NOT_FOUND' });
  }
  res.json(toApiLawyerProfile(result.rows[0]));
}));

/**
 * Create or update the caller's professional profile.
 *
 * Re-submitting after a rejection or a request for more information resets the
 * status to PENDING so the application re-enters the admin queue. A profile that
 * is already VERIFIED or SUSPENDED keeps its status: a verified lawyer editing
 * their bio must not silently drop out of the directory, and a suspended one
 * must not be able to clear their own suspension by saving the form.
 */
router.put('/me', ...lawyerSelf, wrap(async (req, res) => {
  const b = req.body || {};

  const fullName = String(b.fullName || '').trim();
  const regNumber = String(b.barCouncilRegNumber || '').trim();
  const barState = String(b.barCouncilState || '').trim();

  if (!fullName || !regNumber || !barState) {
    return res.status(400).json({
      error: 'Full name, bar council registration number and state are required.',
      code: 'VALIDATION_FAILED'
    });
  }

  const existing = await pool.query(
    'SELECT verification_status FROM lawyer_profiles WHERE user_id = $1',
    [req.userId]
  );
  const current = existing.rows[0] ? existing.rows[0].verification_status : null;
  const resetsToPending = current === null || current === 'REJECTED' || current === 'MORE_INFO_REQUESTED';
  const nextStatus = resetsToPending ? 'PENDING' : current;

  const params = [
    req.userId, fullName, regNumber, barState,
    b.enrollmentYear != null ? Number(b.enrollmentYear) : null,
    b.yearsExperience != null ? Number(b.yearsExperience) : 0,
    asStringArray(b.specializations),
    asStringArray(b.languages),
    b.practiceState != null ? String(b.practiceState).trim() : null,
    b.practiceDistrict != null ? String(b.practiceDistrict).trim() : null,
    b.bio != null ? String(b.bio).trim() : null,
    b.contactEmail != null ? String(b.contactEmail).trim() : null,
    b.contactPhone != null ? String(b.contactPhone).trim() : null,
    nextStatus
  ];

  try {
    const result = await pool.query(
      `INSERT INTO lawyer_profiles (
         user_id, full_name, bar_council_reg_number, bar_council_state,
         enrollment_year, years_experience, specializations, languages,
         practice_state, practice_district, bio, contact_email, contact_phone,
         verification_status, updated_at
       ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14, now())
       ON CONFLICT (user_id) DO UPDATE SET
         full_name = EXCLUDED.full_name,
         bar_council_reg_number = EXCLUDED.bar_council_reg_number,
         bar_council_state = EXCLUDED.bar_council_state,
         enrollment_year = EXCLUDED.enrollment_year,
         years_experience = EXCLUDED.years_experience,
         specializations = EXCLUDED.specializations,
         languages = EXCLUDED.languages,
         practice_state = EXCLUDED.practice_state,
         practice_district = EXCLUDED.practice_district,
         bio = EXCLUDED.bio,
         contact_email = EXCLUDED.contact_email,
         contact_phone = EXCLUDED.contact_phone,
         verification_status = EXCLUDED.verification_status,
         verification_notes = CASE
           WHEN EXCLUDED.verification_status = 'PENDING' THEN NULL
           ELSE lawyer_profiles.verification_notes END,
         updated_at = now()
       RETURNING *`,
      params
    );
    res.json(toApiLawyerProfile(result.rows[0]));
  } catch (err) {
    // Partial unique index on (state, upper(reg_number)) for live applications.
    if (err.code === '23505') {
      return res.status(409).json({
        error: 'That bar council registration number is already registered in this state.',
        code: 'DUPLICATE_REGISTRATION'
      });
    }
    throw err;
  }
}));

router.get('/me/documents', ...lawyerSelf, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT id, doc_type, file_name, mime_type, byte_size, uploaded_at
       FROM lawyer_verification_documents
      WHERE lawyer_user_id = $1
      ORDER BY uploaded_at DESC`,
    [req.userId]
  );
  res.json(result.rows.map(toApiVerificationDocument));
}));

router.post('/me/documents', ...lawyerSelf, upload.single('file'), wrap(async (req, res) => {
  const docType = String((req.body || {}).docType || '').trim();
  if (!docType || !req.file) {
    return res.status(400).json({ error: 'docType and file are required.', code: 'VALIDATION_FAILED' });
  }

  const result = await pool.query(
    `INSERT INTO lawyer_verification_documents
       (lawyer_user_id, doc_type, file_name, mime_type, file_data, byte_size)
     VALUES ($1,$2,$3,$4,$5,$6)
     RETURNING id, doc_type, file_name, mime_type, byte_size, uploaded_at`,
    [req.userId, docType, req.file.originalname, req.file.mimetype, req.file.buffer, req.file.size]
  );
  res.status(201).json(toApiVerificationDocument(result.rows[0]));
}));

/**
 * Delete one of the caller's own verification documents.
 *
 * Blocked once VERIFIED: the documents are the evidence the approval rests on,
 * so they must remain available for audit after the fact.
 */
router.delete('/me/documents/:id', ...lawyerSelf, wrap(async (req, res) => {
  const profile = await pool.query(
    'SELECT verification_status FROM lawyer_profiles WHERE user_id = $1',
    [req.userId]
  );
  const status = profile.rows[0] ? profile.rows[0].verification_status : null;
  if (status === 'VERIFIED' || status === 'SUSPENDED') {
    return res.status(409).json({
      error: 'Verification documents cannot be removed after review.',
      code: 'DOCUMENTS_LOCKED'
    });
  }

  const result = await pool.query(
    'DELETE FROM lawyer_verification_documents WHERE id = $1 AND lawyer_user_id = $2 RETURNING id',
    [req.params.id, req.userId]
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Document not found.', code: 'NOT_FOUND' });
  }
  res.status(204).send();
}));

module.exports = router;
