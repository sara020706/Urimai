const express = require('express');
const multer = require('multer');
const pool = require('../db/pool');
const { authed } = require('../middleware/auth');
const { wrap } = require('../lib/async');
const { ocrLimiter, perUser } = require('../lib/rateLimit');
const gemini = require('../lib/gemini');

const router = express.Router();

/**
 * AI assistance endpoints.
 *
 * ========================== AI IS NOT THE DECIDER ===========================
 * The eligibility verdict is computed by the deterministic rule engine on the
 * device and passed in. Nothing here recomputes it. A model outage,
 * hallucination or rate limit can degrade the WORDING of an explanation; it
 * cannot change who is eligible.
 *
 * Likewise /ocr/extract-profile is advisory: it returns candidate fields for
 * the user to confirm and never writes user_profiles. The user confirms, then
 * the existing PUT /profile applies the change.
 * ============================================================================
 */

const ALLOWED_OCR_MIME = new Set([
  'image/jpeg', 'image/png', 'image/webp', 'application/pdf'
]);

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (_req, file, cb) => {
    if (ALLOWED_OCR_MIME.has(file.mimetype)) return cb(null, true);
    const err = new Error('Unsupported file type.');
    err.code = 'UNSUPPORTED_FILE_TYPE';
    cb(err);
  }
});

const explainLimiter = perUser(60, 60 * 60 * 1000);
const chatLimiter = perUser(60, 60 * 60 * 1000);

/** Map a Gemini failure to a client response. 503 means "use your fallback". */
function handleGeminiError(err, res) {
  if (err.code === 'GEMINI_NOT_CONFIGURED') {
    return res.status(503).json({
      error: 'AI assistance is not configured on this server.',
      code: 'AI_UNAVAILABLE'
    });
  }
  if (err.code === 'GEMINI_FAILED') {
    return res.status(503).json({
      error: 'AI assistance is temporarily unavailable.',
      code: 'AI_UNAVAILABLE'
    });
  }
  throw err;
}

const LANGUAGE_NAMES = { en: 'English', ta: 'Tamil', hi: 'Hindi' };

function languageName(code) {
  return LANGUAGE_NAMES[String(code || 'en').toLowerCase()] || 'English';
}

/** Clamp and stringify a criteria list supplied by the client. */
function formatCriteria(list, cap = 12) {
  if (!Array.isArray(list)) return 'None';
  const items = list.slice(0, cap).map((c) => {
    const title = String(c && c.title ? c.title : '').slice(0, 120);
    const detail = String(c && c.detail ? c.detail : '').slice(0, 200);
    return detail ? `- ${title}: ${detail}` : `- ${title}`;
  });
  return items.length ? items.join('\n') : 'None';
}

/**
 * Explain an already-computed eligibility verdict in plain language.
 *
 * The caller sends the verdict and the per-criterion breakdown. The model is
 * asked only to narrate it.
 */
router.post('/explain-scheme', ...authed, explainLimiter, wrap(async (req, res) => {
  const b = req.body || {};
  const schemeName = String(b.schemeName || '').trim();
  const status = String(b.status || '').trim();

  if (!schemeName || !status) {
    return res.status(400).json({
      error: 'schemeName and status are required.',
      code: 'VALIDATION_FAILED'
    });
  }

  const prompt = `Scheme: ${schemeName}
Department: ${String(b.department || 'Not specified').slice(0, 200)}
Benefit: ${String(b.benefitHighlight || 'Not specified').slice(0, 300)}

The eligibility engine has ALREADY determined this result: ${status}

Criteria the citizen satisfies:
${formatCriteria(b.passed)}

Criteria that failed:
${formatCriteria(b.failed)}

Criteria needing more information:
${formatCriteria(b.missing)}

Explain this result to the citizen in ${languageName(b.language)}.
Rules:
- Do NOT change or second-guess the result above; it is authoritative.
- Open with what the result means for them in one sentence.
- If something failed, say exactly what and by how much where a number is given.
- If information is missing, say precisely what to provide.
- End with the single most useful next step.
- Plain language, no legal jargon, under 180 words.
- Never promise approval; final eligibility rests with the government authority.`;

  try {
    const text = await gemini.generateContent([{ text: prompt }], { temperature: 0.4 });
    res.json({ explanation: text.trim() });
  } catch (err) {
    return handleGeminiError(err, res);
  }
}));

/**
 * Answer a follow-up question, scoped to one scheme.
 */
router.post('/scheme-chat', ...authed, chatLimiter, wrap(async (req, res) => {
  const b = req.body || {};
  const question = String(b.question || '').trim();
  const schemeName = String(b.schemeName || '').trim();

  if (!question || !schemeName) {
    return res.status(400).json({
      error: 'question and schemeName are required.',
      code: 'VALIDATION_FAILED'
    });
  }
  if (question.length > 1000) {
    return res.status(400).json({ error: 'Question is too long.', code: 'VALIDATION_FAILED' });
  }

  const prompt = `You are Urimai's assistant, helping an Indian citizen understand one government scheme.

Scheme: ${schemeName}
Department: ${String(b.department || 'Not specified').slice(0, 200)}
Their eligibility result: ${String(b.status || 'Not evaluated')}

Criteria satisfied:
${formatCriteria(b.passed, 8)}

Criteria not satisfied:
${formatCriteria(b.failed, 8)}

Required documents: ${
    Array.isArray(b.documents) ? b.documents.slice(0, 15).join(', ') : 'Not specified'
  }

The citizen asks: "${question}"

Answer in ${languageName(b.language)}, under 150 words, grounded ONLY in the
scheme information above. If the answer is not in that information, say so and
point them to the official portal rather than guessing. Never promise approval.`;

  try {
    const text = await gemini.generateContent([{ text: prompt }], { temperature: 0.5 });
    res.json({ answer: text.trim() });
  } catch (err) {
    return handleGeminiError(err, res);
  }
}));

/** Fields the extractor may return. Anything else is discarded. */
const PROFILE_FIELDS = [
  'name', 'age', 'gender', 'state', 'district', 'occupation', 'education',
  'annualIncome', 'familySize', 'socialCategory', 'disabilityStatus', 'maritalStatus'
];

const EXTRACTION_SCHEMA = `{
  "name": "string or null",
  "age": "integer or null",
  "gender": "Male | Female | Other | null",
  "state": "string or null",
  "district": "string or null",
  "occupation": "Student | Employed | Self-Employed | Unemployed | Farmer | Homemaker | null",
  "education": "Below 10th | 10th Pass | 12th Pass | Diploma | Undergraduate | Postgraduate | Doctorate | null",
  "annualIncome": "integer in INR or null",
  "familySize": "integer or null",
  "socialCategory": "string or null",
  "disabilityStatus": "string or null",
  "maritalStatus": "Single | Married | Widowed | Divorced/Separated | null",
  "documentType": "string describing the certificate, or null",
  "confidence": "high | medium | low"
}`;

/**
 * Keep only known fields with plausible values.
 *
 * The model's output is never trusted into a typed field: an out-of-range age
 * or a negative income would otherwise reach the profile editor and look like
 * something the citizen entered.
 */
function sanitizeExtraction(raw) {
  const out = {};
  if (!raw || typeof raw !== 'object') return out;

  for (const field of PROFILE_FIELDS) {
    const value = raw[field];
    if (value === null || value === undefined || value === '') continue;

    if (field === 'age') {
      const n = Number(value);
      if (Number.isInteger(n) && n > 0 && n < 120) out.age = n;
    } else if (field === 'annualIncome') {
      const n = Number(value);
      if (Number.isFinite(n) && n >= 0 && n < 100000000) out.annualIncome = Math.round(n);
    } else if (field === 'familySize') {
      const n = Number(value);
      if (Number.isInteger(n) && n > 0 && n < 50) out.familySize = n;
    } else if (typeof value === 'string') {
      const trimmed = value.trim().slice(0, 120);
      if (trimmed) out[field] = trimmed;
    }
  }
  return out;
}

/**
 * Extract profile fields from a government certificate.
 *
 * Accepts either an already-uploaded documentId or a multipart file. Returns
 * CANDIDATES for the citizen to review; it never writes user_profiles. OCR is
 * wrong often enough that silently trusting it would put incorrect income or
 * category data behind an eligibility verdict.
 */
router.post(
  '/ocr/extract-profile',
  ...authed,
  ocrLimiter,
  upload.single('file'),
  wrap(async (req, res) => {
    let buffer = null;
    let mimeType = null;

    const documentId = (req.body || {}).documentId;
    if (documentId) {
      // Ownership in the WHERE clause: a valid token for user A cannot read
      // user B's document by guessing an id.
      const result = await pool.query(
        'SELECT mime_type, file_data FROM uploaded_documents WHERE id = $1 AND user_id = $2',
        [documentId, req.userId]
      );
      if (result.rowCount === 0) {
        return res.status(404).json({ error: 'Document not found.', code: 'NOT_FOUND' });
      }
      buffer = result.rows[0].file_data;
      mimeType = result.rows[0].mime_type;
    } else if (req.file) {
      buffer = req.file.buffer;
      mimeType = req.file.mimetype;
    } else {
      return res.status(400).json({
        error: 'Provide documentId or upload a file.',
        code: 'VALIDATION_FAILED'
      });
    }

    if (!ALLOWED_OCR_MIME.has(mimeType)) {
      return res.status(415).json({
        error: 'Unsupported file type. Use JPEG, PNG, WebP or PDF.',
        code: 'UNSUPPORTED_FILE_TYPE'
      });
    }

    const prompt = `You are reading an Indian government certificate or identity document.

Extract only what is clearly legible. Use null for anything absent or uncertain —
a null is far better than a guess, because a wrong value here would feed an
eligibility decision.

Return ONLY JSON matching this schema:
${EXTRACTION_SCHEMA}

Notes:
- Convert income written as "2.5 lakh" or "Rs. 2,50,000" to the integer 250000.
- Use the certificate's own spelling for state and district.
- Set confidence to "low" if the scan is unclear or partially illegible.`;

    try {
      const text = await gemini.generateContent(
        [
          { text: prompt },
          { inline_data: { mime_type: mimeType, data: buffer.toString('base64') } }
        ],
        { temperature: 0.1, responseMimeType: 'application/json' }
      );

      let parsed;
      try {
        parsed = gemini.parseJsonResponse(text);
      } catch (_) {
        return res.status(502).json({
          error: 'Could not read that document. Try a clearer photo.',
          code: 'EXTRACTION_UNPARSEABLE'
        });
      }

      res.json({
        fields: sanitizeExtraction(parsed),
        documentType: parsed.documentType ? String(parsed.documentType).slice(0, 120) : null,
        confidence: ['high', 'medium', 'low'].includes(parsed.confidence)
          ? parsed.confidence
          : 'low',
        // Stated plainly so no client is tempted to auto-apply these.
        advisory: 'Extracted automatically. Review and correct every field before saving.'
      });
    } catch (err) {
      return handleGeminiError(err, res);
    }
  })
);

/** Lets the client decide whether to offer AI features at all. */
router.get('/status', ...authed, wrap(async (_req, res) => {
  res.json({ available: gemini.isConfigured(), model: gemini.DEFAULT_MODEL });
}));

module.exports = router;
