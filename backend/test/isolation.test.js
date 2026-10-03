/**
 * Answer-isolation integration test.
 *
 * The design argument for Q&A privacy is that Lawyer A cannot reach Lawyer B's
 * answer by ANY route, including by enumerating ids. An argument is not a
 * guarantee, so this exercises the real HTTP surface against the real database:
 * two verified lawyers answer the same question, then each tries every path
 * that might leak the other's work.
 *
 * Run with the backend NOT already running on TEST_PORT:
 *   node test/isolation.test.js
 */

require('dotenv').config();
const http = require('http');
const pool = require('../src/db/pool');

const PORT = Number(process.env.TEST_PORT || 4199);
const BASE = `http://localhost:${PORT}`;
const STAMP = `iso${Date.now()}`;

let passed = 0;
let failed = 0;
const failures = [];

function check(label, condition, detail) {
  if (condition) {
    passed += 1;
    console.log(`  PASS  ${label}`);
  } else {
    failed += 1;
    failures.push(label + (detail ? ` — ${detail}` : ''));
    console.log(`  FAIL  ${label}${detail ? ` — ${detail}` : ''}`);
  }
}

function request(method, path, { token, body } = {}) {
  return new Promise((resolve, reject) => {
    const payload = body ? Buffer.from(JSON.stringify(body)) : null;
    const headers = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (payload) {
      headers['Content-Type'] = 'application/json';
      headers['Content-Length'] = payload.length;
    }
    const req = http.request({ host: 'localhost', port: PORT, path, method, headers }, (res) => {
      let data = '';
      res.on('data', (c) => { data += c; });
      res.on('end', () => {
        let parsed = null;
        try { parsed = data ? JSON.parse(data) : null; } catch (_) { parsed = data; }
        resolve({ status: res.statusCode, body: parsed, raw: data });
      });
    });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}

async function signup(username, role) {
  const res = await request('POST', '/auth/signup', {
    body: { username, password: 'test1234', displayName: username, role }
  });
  if (res.status === 429) {
    throw new Error(
      'Signup was rate limited. These suites create several accounts per run; ' +
      'start the server with RATE_LIMIT_DISABLED=true, or wait 15 minutes.'
    );
  }
  if (res.status !== 201) throw new Error(`signup ${username}: ${res.raw}`);
  return { token: res.body.token, userId: res.body.userId };
}

/** Promote to VERIFIED directly; the admin approval path is covered elsewhere. */
async function makeVerifiedLawyer(username, userId) {
  await pool.query(
    `INSERT INTO lawyer_profiles
       (user_id, full_name, bar_council_reg_number, bar_council_state, verification_status)
     VALUES ($1, $2, $3, 'Tamil Nadu', 'VERIFIED')
     ON CONFLICT (user_id) DO UPDATE SET verification_status = 'VERIFIED'`,
    [userId, username, `${STAMP}/${userId}`]
  );
}

async function cleanup(usernames) {
  const { rows } = await pool.query('SELECT id FROM users WHERE username = ANY($1)', [usernames]);
  const ids = rows.map((r) => r.id);
  if (ids.length) {
    await pool.query('DELETE FROM admin_audit_log WHERE admin_user_id = ANY($1)', [ids]);
    await pool.query('DELETE FROM users WHERE id = ANY($1)', [ids]);
  }
}

async function main() {
  const names = [`${STAMP}citizen`, `${STAMP}other`, `${STAMP}lawa`, `${STAMP}lawb`];

  console.log(`\nAnswer isolation test (port ${PORT})\n`);

  const citizen = await signup(names[0], 'USER');
  const other = await signup(names[1], 'USER');
  const lawyerA = await signup(names[2], 'LAWYER');
  const lawyerB = await signup(names[3], 'LAWYER');
  await makeVerifiedLawyer(names[2], lawyerA.userId);
  await makeVerifiedLawyer(names[3], lawyerB.userId);

  // --- Setup: one question, answered independently by both lawyers ---------
  const created = await request('POST', '/questions', {
    token: citizen.token,
    body: {
      title: 'My scheme application was rejected on income grounds',
      body: 'The office said my income certificate shows too much. What are my options for appeal?',
      category: 'Welfare',
      state: 'Tamil Nadu'
    }
  });
  check('citizen can post a question', created.status === 201, `status ${created.status}`);
  const questionId = created.body.id;

  const ansA = await request('POST', `/lawyer/questions/${questionId}/answers`, {
    token: lawyerA.token,
    body: { body: 'ANSWER-FROM-LAWYER-A: you may file an appeal with the revenue divisional officer.' }
  });
  check('lawyer A can answer', ansA.status === 201, `status ${ansA.status}`);
  const answerAId = ansA.body && ansA.body.id;

  const ansB = await request('POST', `/lawyer/questions/${questionId}/answers`, {
    token: lawyerB.token,
    body: { body: 'ANSWER-FROM-LAWYER-B: request a re-assessment of the income certificate first.' }
  });
  check('lawyer B can answer', ansB.status === 201, `status ${ansB.status}`);
  const answerBId = ansB.body && ansB.body.id;

  // --- The core property ---------------------------------------------------
  console.log('\n  -- isolation --');

  const detailForA = await request('GET', `/lawyer/questions/${questionId}`, { token: lawyerA.token });
  const aSeesOwn = JSON.stringify(detailForA.body).includes('ANSWER-FROM-LAWYER-A');
  const aSeesOther = JSON.stringify(detailForA.body).includes('ANSWER-FROM-LAWYER-B');
  check('lawyer A sees their own answer on the question detail', aSeesOwn);
  check("lawyer A CANNOT see lawyer B's answer on the question detail", !aSeesOther);

  const detailForB = await request('GET', `/lawyer/questions/${questionId}`, { token: lawyerB.token });
  check("lawyer B CANNOT see lawyer A's answer on the question detail",
    !JSON.stringify(detailForB.body).includes('ANSWER-FROM-LAWYER-A'));

  const mineA = await request('GET', '/lawyer/answers/mine', { token: lawyerA.token });
  check('lawyer A\'s own-answers list contains only their own',
    !JSON.stringify(mineA.body).includes('ANSWER-FROM-LAWYER-B'));

  const feedA = await request('GET', '/lawyer/questions', { token: lawyerA.token });
  check('feed never carries answer bodies',
    !JSON.stringify(feedA.body).includes('ANSWER-FROM-LAWYER-'));

  // --- Enumeration ---------------------------------------------------------
  console.log('\n  -- id enumeration --');

  const editOther = await request('PUT', `/lawyer/answers/${answerBId}`, {
    token: lawyerA.token,
    body: { body: 'HIJACKED BY LAWYER A - this must never be written to the database.' }
  });
  check("lawyer A cannot edit lawyer B's answer by id", editOther.status === 404,
    `status ${editOther.status}`);

  const deleteOther = await request('DELETE', `/lawyer/answers/${answerBId}`, { token: lawyerA.token });
  check("lawyer A cannot delete lawyer B's answer by id", deleteOther.status === 404,
    `status ${deleteOther.status}`);

  const stillIntact = await pool.query('SELECT body FROM legal_answers WHERE id = $1', [answerBId]);
  check("lawyer B's answer is unchanged in the database",
    stillIntact.rowCount === 1 && stillIntact.rows[0].body.includes('ANSWER-FROM-LAWYER-B'));

  // Sweep ids on every route that accepts one, looking for a write that lands
  // on a row Lawyer A does not own.
  //
  // Lawyer A's OWN answer id is excluded. Editing it is legitimate, so a 200
  // there proves nothing — and overwriting it would destroy the content the
  // asker-visibility assertion below depends on. The question under test is
  // whether a FOREIGN row can be written.
  let leaked = false;
  const sweepIds = [];
  for (let i = 1; i <= 40; i += 1) {
    if (String(i) !== String(answerAId)) sweepIds.push(String(i));
  }
  if (answerBId) sweepIds.push(String(answerBId));

  for (const id of sweepIds) {
    const edit = await request('PUT', `/lawyer/answers/${id}`, {
      token: lawyerA.token,
      body: { body: 'sweeping for a writable answer that is not mine at all.' }
    });
    if (edit.status === 200) {
      // A 200 is acceptable only if the row genuinely belongs to Lawyer A.
      const owner = await pool.query(
        'SELECT lawyer_user_id FROM legal_answers WHERE id = $1', [id]
      );
      if (!owner.rowCount || String(owner.rows[0].lawyer_user_id) !== String(lawyerA.userId)) {
        leaked = true;
      }
    }
  }
  check('sweeping answer ids never writes to another lawyer\'s answer', !leaked);

  const noSuchRoute = await request('GET', `/answers/${answerBId}`, { token: lawyerA.token });
  check('there is no GET /answers/:id route at all', noSuchRoute.status === 404,
    `status ${noSuchRoute.status}`);

  // --- The asker sees everything ------------------------------------------
  console.log('\n  -- asker visibility --');

  const askerView = await request('GET', `/questions/${questionId}`, { token: citizen.token });
  const askerText = JSON.stringify(askerView.body);
  const sawA = askerText.includes('ANSWER-FROM-LAWYER-A');
  const sawB = askerText.includes('ANSWER-FROM-LAWYER-B');
  check('asker sees BOTH answers', sawA && sawB,
    `sawA=${sawA} sawB=${sawB} returned=${(askerView.body.answers || []).length}`);
  check('asker sees the answer count', askerView.body.question.answerCount === 2,
    `count ${askerView.body.question && askerView.body.question.answerCount}`);

  const otherCitizen = await request('GET', `/questions/${questionId}`, { token: other.token });
  check('an unrelated citizen gets 404, not 403', otherCitizen.status === 404,
    `status ${otherCitizen.status}`);

  const otherList = await request('GET', '/questions/mine', { token: other.token });
  check('an unrelated citizen does not see the question in their list',
    !JSON.stringify(otherList.body).includes(String(questionId)));

  // --- Anonymity -----------------------------------------------------------
  console.log('\n  -- anonymity --');

  const lawyerPayloads = JSON.stringify(detailForA.body) + JSON.stringify(feedA.body) +
    JSON.stringify(mineA.body);
  check('no lawyer-facing response contains askerUserId',
    !/askerUserId/i.test(lawyerPayloads));
  check('no lawyer-facing response contains the asker\'s user id value',
    !lawyerPayloads.includes(`"${citizen.userId}"`));
  check('no lawyer-facing response contains the asker\'s username',
    !lawyerPayloads.includes(names[0]));
  check('no lawyer-facing response leaks answerCount',
    !/answerCount/i.test(lawyerPayloads));

  // --- Counter integrity ---------------------------------------------------
  console.log('\n  -- counters and duplicates --');

  const dupe = await request('POST', `/lawyer/questions/${questionId}/answers`, {
    token: lawyerA.token,
    body: { body: 'A second answer from the same lawyer should be refused outright.' }
  });
  check('a lawyer cannot answer the same question twice', dupe.status === 409,
    `status ${dupe.status}`);

  const countRow = await pool.query(
    `SELECT q.answer_count, (SELECT count(*)::int FROM legal_answers a WHERE a.question_id = q.id) AS actual
       FROM legal_questions q WHERE q.id = $1`,
    [questionId]
  );
  check('answer_count matches the real row count',
    countRow.rows[0].answer_count === countRow.rows[0].actual,
    `${countRow.rows[0].answer_count} vs ${countRow.rows[0].actual}`);

  await request('DELETE', `/lawyer/answers/${answerAId}`, { token: lawyerA.token });
  const afterDelete = await pool.query(
    `SELECT q.answer_count, (SELECT count(*)::int FROM legal_answers a WHERE a.question_id = q.id) AS actual
       FROM legal_questions q WHERE q.id = $1`,
    [questionId]
  );
  check('answer_count stays correct after a delete',
    afterDelete.rows[0].answer_count === afterDelete.rows[0].actual,
    `${afterDelete.rows[0].answer_count} vs ${afterDelete.rows[0].actual}`);

  // --- Unverified lawyers --------------------------------------------------
  console.log('\n  -- verification gating --');

  await pool.query(
    `UPDATE lawyer_profiles SET verification_status = 'PENDING' WHERE user_id = $1`,
    [lawyerB.userId]
  );
  const pendingFeed = await request('GET', '/lawyer/questions', { token: lawyerB.token });
  check('a PENDING lawyer cannot read the question feed', pendingFeed.status === 403,
    `status ${pendingFeed.status}`);
  check('the refusal names the reason',
    pendingFeed.body && pendingFeed.body.code === 'LAWYER_NOT_VERIFIED');

  const pendingAnswer = await request('PUT', `/lawyer/answers/${answerBId}`, {
    token: lawyerB.token,
    body: { body: 'An unverified lawyer must not be able to edit even their own answer.' }
  });
  check('a PENDING lawyer cannot edit their own existing answer', pendingAnswer.status === 403,
    `status ${pendingAnswer.status}`);

  const citizenFeed = await request('GET', '/lawyer/questions', { token: citizen.token });
  check('a citizen cannot read the lawyer feed', citizenFeed.status === 403,
    `status ${citizenFeed.status}`);

  // --- Done ----------------------------------------------------------------
  await cleanup(names);

  console.log(`\n${passed} passed, ${failed} failed\n`);
  if (failures.length) {
    console.log('FAILURES:');
    failures.forEach((f) => console.log('  - ' + f));
  }
  await pool.end();
  process.exit(failed === 0 ? 0 : 1);
}

main().catch(async (err) => {
  console.error('\nTest harness error:', err.message);
  try { await cleanup([`${STAMP}citizen`, `${STAMP}other`, `${STAMP}lawa`, `${STAMP}lawb`]); } catch (_) {}
  try { await pool.end(); } catch (_) {}
  process.exit(1);
});
