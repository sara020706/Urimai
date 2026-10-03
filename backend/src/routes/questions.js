const express = require('express');
const pool = require('../db/pool');
const { authed } = require('../middleware/auth');
const { wrap } = require('../lib/async');
const { questionLimiter } = require('../lib/rateLimit');

const router = express.Router();

/**
 * Citizen-facing legal Q&A.
 *
 * ============================ ISOLATION INVARIANT ============================
 * A row of legal_answers is readable by exactly two principals: its author
 * (legal_answers.lawyer_user_id) and the asker of its question
 * (legal_questions.asker_user_id). Plus ADMIN, via a separate audited route.
 *
 * Every statement touching legal_answers is constrained in its WHERE/ON clause.
 * Nothing is fetched broadly and filtered in JS — that pattern has a "forgot the
 * filter" failure mode and leaves other people's data sitting in memory.
 *
 * See also routes/lawyerQuestions.js. Between the two files, legal_answers is
 * referenced in exactly five statements; LegalAnswerIsolation.test asserts that
 * no sixth appears.
 * =============================================================================
 */

function toApiQuestionSummary(row) {
  return {
    id: String(row.id),
    title: row.title,
    category: row.category,
    state: row.state,
    language: row.language,
    status: row.status,
    answerCount: row.answer_count,
    createdAt: new Date(row.created_at).getTime()
  };
}

/**
 * Ask a question.
 *
 * The asker's identity is recorded (they must be able to find their own
 * question and read its answers) but is never exposed to lawyers.
 */
router.post('/', ...authed, questionLimiter, wrap(async (req, res) => {
  const { title, body, category, state, language } = req.body || {};
  const cleanTitle = String(title || '').trim();
  const cleanBody = String(body || '').trim();

  if (cleanTitle.length < 10 || cleanBody.length < 20) {
    return res.status(400).json({
      error: 'Please give a title of at least 10 characters and a description of at least 20.',
      code: 'VALIDATION_FAILED'
    });
  }
  if (cleanTitle.length > 200 || cleanBody.length > 5000) {
    return res.status(400).json({ error: 'Question is too long.', code: 'VALIDATION_FAILED' });
  }

  const result = await pool.query(
    `INSERT INTO legal_questions (asker_user_id, title, body, category, state, language)
     VALUES ($1,$2,$3,$4,$5,$6)
     RETURNING id, title, category, state, language, status, answer_count, created_at`,
    [
      req.userId, cleanTitle, cleanBody,
      category ? String(category).trim() : null,
      state ? String(state).trim() : null,
      language ? String(language).trim() : 'en'
    ]
  );
  res.status(201).json(toApiQuestionSummary(result.rows[0]));
}));

/**
 * The caller's own questions.
 *
 * answer_count IS exposed here: it is the asker's own data about their own
 * question, and it drives the "3 lawyers have replied" UI. It is deliberately
 * absent from every lawyer-facing projection — see lawyerQuestions.js.
 */
router.get('/mine', ...authed, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT id, title, category, state, language, status, answer_count, created_at
       FROM legal_questions
      WHERE asker_user_id = $1
      ORDER BY created_at DESC`,
    [req.userId]
  );
  res.json(result.rows.map(toApiQuestionSummary));
}));

/**
 * One of the caller's own questions, with every answer it has received.
 *
 * Two statements, and the ownership predicate is REPEATED in the second rather
 * than inherited from the first. If someone later refactors them apart, the
 * answers query is still safe standing alone. One line of defence in depth.
 */
router.get('/:id', ...authed, wrap(async (req, res) => {
  const questionResult = await pool.query(
    `SELECT id, title, body, category, state, language, status, answer_count, created_at
       FROM legal_questions
      WHERE id = $1 AND asker_user_id = $2`,
    [req.params.id, req.userId]
  );

  // 404 rather than 403: a 403 would confirm the question exists, and ids are
  // sequential, so that alone would leak the shape of the table.
  if (questionResult.rowCount === 0) {
    return res.status(404).json({ error: 'Question not found.', code: 'NOT_FOUND' });
  }

  const answersResult = await pool.query(
    `SELECT a.id, a.body, a.created_at, a.updated_at,
            lp.full_name, lp.years_experience, lp.specializations,
            a.lawyer_user_id
       FROM legal_answers a
       JOIN legal_questions q ON q.id = a.question_id
       JOIN lawyer_profiles lp ON lp.user_id = a.lawyer_user_id
      WHERE a.question_id = $1
        AND q.asker_user_id = $2
        AND a.is_hidden = FALSE
      ORDER BY a.created_at ASC`,
    [req.params.id, req.userId]
  );

  const q = questionResult.rows[0];
  res.json({
    question: {
      id: String(q.id),
      title: q.title,
      body: q.body,
      category: q.category,
      state: q.state,
      language: q.language,
      status: q.status,
      answerCount: q.answer_count,
      createdAt: new Date(q.created_at).getTime()
    },
    answers: answersResult.rows.map((a) => ({
      id: String(a.id),
      body: a.body,
      lawyerUserId: String(a.lawyer_user_id),
      lawyerName: a.full_name,
      lawyerYearsExperience: a.years_experience,
      lawyerSpecializations: a.specializations || [],
      createdAt: new Date(a.created_at).getTime(),
      updatedAt: new Date(a.updated_at).getTime()
    }))
  });
}));

router.post('/:id/close', ...authed, wrap(async (req, res) => {
  const result = await pool.query(
    `UPDATE legal_questions SET status = 'CLOSED', updated_at = now()
      WHERE id = $1 AND asker_user_id = $2 AND status <> 'REMOVED'
      RETURNING id`,
    [req.params.id, req.userId]
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Question not found.', code: 'NOT_FOUND' });
  }
  res.status(204).send();
}));

module.exports = router;
