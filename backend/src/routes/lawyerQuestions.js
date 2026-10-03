const express = require('express');
const pool = require('../db/pool');
const { lawyerOnly } = require('../middleware/auth');
const { wrap } = require('../lib/async');

const router = express.Router();

/**
 * Lawyer-facing Q&A. Every route here requires a VERIFIED lawyer.
 *
 * ============================ ISOLATION INVARIANT ============================
 * Lawyer A must never be able to read Lawyer B's answer, by any route,
 * including by guessing ids.
 *
 * Three structural decisions enforce that, in order of importance:
 *
 *   1. THERE IS NO GET /answers/:id. An id-keyed read endpoint would add zero
 *      capability — a lawyer reaches their own answer through
 *      (question_id, me), which they already know — while adding the entire
 *      IDOR surface. Declining to build it is the strongest mitigation
 *      available, and it is why you will not find such a route below.
 *
 *   2. Every reference to legal_answers is pinned to the caller, either by
 *      `a.lawyer_user_id = $me` in the ON/WHERE clause, or (in questions.js)
 *      by a join carrying `q.asker_user_id = $me`. There is no third form.
 *
 *   3. Where an answer id must be accepted at all (edit, delete), it is paired
 *      with the ownership predicate in the SAME statement, so there is no
 *      fetch-check-write window and no TOCTOU.
 *
 * Note also what is ABSENT from every projection here: asker_user_id (that is
 * the anonymity guarantee) and answer_count (see the comment on the feed).
 * =============================================================================
 */

/**
 * The question feed.
 *
 * The only reference to legal_answers is a LEFT JOIN whose ON clause is pinned
 * to the calling lawyer, so the planner cannot produce a row from anyone else's
 * answer. No column of `a` reaches the client except the derived boolean.
 *
 * answer_count is deliberately NOT selected. A live count of how many lawyers
 * have already answered is a real, if subtle, timing channel: watching it move
 * 2 -> 3 reveals when a competitor submitted, and sampled across many questions
 * it supports traffic analysis of who is active when. It also causes anchoring
 * and racing, which starves older questions. And it does not help a lawyer write
 * a better answer. Something that leaks a little and helps nothing gets cut.
 * `iHaveAnswered` is derived purely from the caller's own row.
 */
router.get('/questions', ...lawyerOnly, wrap(async (req, res) => {
  const limit = Math.min(Number(req.query.limit) || 50, 100);
  const offset = Math.max(Number(req.query.offset) || 0, 0);

  const result = await pool.query(
    `SELECT q.id, q.title, q.category, q.state, q.language, q.status, q.created_at,
            (a.id IS NOT NULL) AS i_have_answered
       FROM legal_questions q
       LEFT JOIN legal_answers a
              ON a.question_id = q.id
             AND a.lawyer_user_id = $1
      WHERE q.is_hidden = FALSE
        AND q.status IN ('OPEN','ANSWERED')
        AND ($2::text IS NULL OR q.category = $2)
        AND ($3::text IS NULL OR q.state = $3)
        AND ($4::text IS NULL OR q.language = $4)
      ORDER BY q.created_at DESC
      LIMIT $5 OFFSET $6`,
    [
      req.userId,
      req.query.category || null,
      req.query.state || null,
      req.query.language || null,
      limit,
      offset
    ]
  );

  res.json(result.rows.map((row) => ({
    id: String(row.id),
    title: row.title,
    category: row.category,
    state: row.state,
    language: row.language,
    status: row.status,
    iHaveAnswered: row.i_have_answered,
    createdAt: new Date(row.created_at).getTime()
  })));
}));

/**
 * One question, plus the caller's own answer if they have written one.
 *
 * This is a physically separate query from the asker's view in questions.js,
 * not one query with a role conditional. A conditional is one edit away from
 * leaking; two queries that never had the capability cannot.
 */
router.get('/questions/:id', ...lawyerOnly, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT q.id, q.title, q.body, q.category, q.state, q.language, q.status, q.created_at,
            a.id AS my_answer_id, a.body AS my_answer_body,
            a.created_at AS my_answered_at, a.updated_at AS my_answer_updated_at
       FROM legal_questions q
       LEFT JOIN legal_answers a
              ON a.question_id = q.id
             AND a.lawyer_user_id = $2
      WHERE q.id = $1
        AND q.is_hidden = FALSE`,
    [req.params.id, req.userId]
  );

  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Question not found.', code: 'NOT_FOUND' });
  }

  const row = result.rows[0];
  res.json({
    question: {
      id: String(row.id),
      title: row.title,
      body: row.body,
      category: row.category,
      state: row.state,
      language: row.language,
      status: row.status,
      createdAt: new Date(row.created_at).getTime()
    },
    myAnswer: row.my_answer_id
      ? {
          id: String(row.my_answer_id),
          body: row.my_answer_body,
          createdAt: new Date(row.my_answered_at).getTime(),
          updatedAt: new Date(row.my_answer_updated_at).getTime()
        }
      : null
  });
}));

/**
 * Answer a question.
 *
 * The insert and the counter increment share one transaction, so answer_count
 * cannot drift from the number of rows. The asker's notification is written in
 * the same transaction too — they are never left unaware of an answer that
 * exists.
 */
router.post('/questions/:id/answers', ...lawyerOnly, wrap(async (req, res) => {
  const body = String((req.body || {}).body || '').trim();
  if (body.length < 20) {
    return res.status(400).json({
      error: 'An answer must be at least 20 characters.',
      code: 'VALIDATION_FAILED'
    });
  }
  if (body.length > 10000) {
    return res.status(400).json({ error: 'Answer is too long.', code: 'VALIDATION_FAILED' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    const question = await client.query(
      `SELECT id, asker_user_id, status FROM legal_questions
        WHERE id = $1 AND is_hidden = FALSE FOR UPDATE`,
      [req.params.id]
    );
    if (question.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Question not found.', code: 'NOT_FOUND' });
    }
    if (question.rows[0].status === 'CLOSED' || question.rows[0].status === 'REMOVED') {
      await client.query('ROLLBACK');
      return res.status(409).json({
        error: 'This question is no longer accepting answers.',
        code: 'QUESTION_CLOSED'
      });
    }

    let inserted;
    try {
      inserted = await client.query(
        `INSERT INTO legal_answers (question_id, lawyer_user_id, body)
         VALUES ($1,$2,$3)
         RETURNING id, body, created_at, updated_at`,
        [req.params.id, req.userId, body]
      );
    } catch (err) {
      if (err.code === '23505') {
        await client.query('ROLLBACK');
        return res.status(409).json({
          error: 'You have already answered this question. Edit your existing answer instead.',
          code: 'ALREADY_ANSWERED'
        });
      }
      throw err;
    }

    await client.query(
      `UPDATE legal_questions
          SET answer_count = answer_count + 1,
              status = CASE WHEN status = 'OPEN' THEN 'ANSWERED' ELSE status END,
              updated_at = now()
        WHERE id = $1`,
      [req.params.id]
    );

    await client.query(
      `INSERT INTO notifications (user_id, type, title, body, target_type, target_id)
       VALUES ($1, 'ANSWER_RECEIVED', 'A lawyer has answered your question', NULL, 'QUESTION', $2)`,
      [question.rows[0].asker_user_id, req.params.id]
    );

    await client.query('COMMIT');

    const a = inserted.rows[0];
    res.status(201).json({
      id: String(a.id),
      body: a.body,
      createdAt: new Date(a.created_at).getTime(),
      updatedAt: new Date(a.updated_at).getTime()
    });
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

/**
 * Edit the caller's own answer.
 *
 * id and ownership in one atomic statement. rowCount === 0 means either the
 * answer does not exist or it belongs to someone else — both answer 404, so the
 * response does not distinguish them.
 */
router.put('/answers/:id', ...lawyerOnly, wrap(async (req, res) => {
  const body = String((req.body || {}).body || '').trim();
  if (body.length < 20) {
    return res.status(400).json({
      error: 'An answer must be at least 20 characters.',
      code: 'VALIDATION_FAILED'
    });
  }

  const result = await pool.query(
    `UPDATE legal_answers SET body = $3, updated_at = now()
      WHERE id = $1 AND lawyer_user_id = $2
      RETURNING id, body, created_at, updated_at`,
    [req.params.id, req.userId, body]
  );

  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Answer not found.', code: 'NOT_FOUND' });
  }
  const a = result.rows[0];
  res.json({
    id: String(a.id),
    body: a.body,
    createdAt: new Date(a.created_at).getTime(),
    updatedAt: new Date(a.updated_at).getTime()
  });
}));

router.delete('/answers/:id', ...lawyerOnly, wrap(async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await client.query(
      'DELETE FROM legal_answers WHERE id = $1 AND lawyer_user_id = $2 RETURNING question_id',
      [req.params.id, req.userId]
    );
    if (result.rowCount === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Answer not found.', code: 'NOT_FOUND' });
    }
    // Keep the counter honest, and never let it go negative.
    await client.query(
      `UPDATE legal_questions
          SET answer_count = GREATEST(answer_count - 1, 0), updated_at = now()
        WHERE id = $1`,
      [result.rows[0].question_id]
    );
    await client.query('COMMIT');
    res.status(204).send();
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}));

/**
 * The caller's own answers. Rooted at the actor, so trivially safe.
 */
router.get('/answers/mine', ...lawyerOnly, wrap(async (req, res) => {
  const result = await pool.query(
    `SELECT a.id, a.body, a.created_at, a.updated_at,
            q.id AS question_id, q.title, q.category, q.status
       FROM legal_answers a
       JOIN legal_questions q ON q.id = a.question_id
      WHERE a.lawyer_user_id = $1
      ORDER BY a.created_at DESC`,
    [req.userId]
  );
  res.json(result.rows.map((row) => ({
    id: String(row.id),
    body: row.body,
    questionId: String(row.question_id),
    questionTitle: row.title,
    questionCategory: row.category,
    questionStatus: row.status,
    createdAt: new Date(row.created_at).getTime(),
    updatedAt: new Date(row.updated_at).getTime()
  })));
}));

module.exports = router;
