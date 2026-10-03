/**
 * Structural guard on the answer-isolation invariant.
 *
 * isolation.test.js proves the CURRENT routes do not leak. The real residual
 * risk is future code: someone adds a convenient endpoint, fetches answers
 * broadly, and filters in JS. That would pass every existing behavioural test
 * on the day it is written and leak the first time the filter is wrong.
 *
 * So this test reads the source. It asserts that legal_answers is referenced
 * only in files that have been reviewed for isolation, and that each reference
 * is constrained to the caller. If you are here because this test failed, do
 * not just add your file to the allowlist — re-read the invariant at the top of
 * routes/lawyerQuestions.js first and make sure your query is pinned to
 * lawyer_user_id = $me or joined through asker_user_id = $me.
 *
 *   node test/answerSurface.test.js
 */

const fs = require('fs');
const path = require('path');

const ROUTES_DIR = path.join(__dirname, '..', 'src', 'routes');

// Files permitted to touch legal_answers, and why.
const ALLOWLIST = {
  'lawyerQuestions.js': 'lawyer-facing Q&A; every reference pinned to lawyer_user_id = $me',
  'questions.js': 'asker-facing; answers reached only through asker_user_id = $me',
  'admin.js': 'moderation; role-gated and audited'
};

// A reference is considered constrained if the surrounding statement carries
// one of these predicates.
const OWNERSHIP_PREDICATES = [
  /lawyer_user_id\s*=\s*\$\d/i,
  /asker_user_id\s*=\s*\$\d/i
];

let passed = 0;
let failed = 0;
const failures = [];

function check(label, ok, detail) {
  if (ok) {
    passed += 1;
    console.log(`  PASS  ${label}`);
  } else {
    failed += 1;
    failures.push(label + (detail ? ` — ${detail}` : ''));
    console.log(`  FAIL  ${label}${detail ? ` — ${detail}` : ''}`);
  }
}

/** Split a file into SQL template literals so each statement is checked alone. */
function sqlStatements(source) {
  const out = [];
  const re = /`([^`]*)`/g;
  let m;
  while ((m = re.exec(source)) !== null) {
    if (/legal_answers/i.test(m[1])) out.push(m[1]);
  }
  return out;
}

console.log('\nAnswer surface guard\n');

const files = fs.readdirSync(ROUTES_DIR).filter((f) => f.endsWith('.js'));
const touching = [];

for (const file of files) {
  const source = fs.readFileSync(path.join(ROUTES_DIR, file), 'utf8');
  if (/legal_answers/i.test(source)) touching.push(file);
}

check(
  'only reviewed files reference legal_answers',
  touching.every((f) => ALLOWLIST[f]),
  `unexpected: ${touching.filter((f) => !ALLOWLIST[f]).join(', ') || 'none'}`
);

// Every statement must be pinned to the caller. admin.js is exempt: it is
// role-gated and audited, and moderation legitimately needs a broad view.
for (const file of touching) {
  if (file === 'admin.js') continue;
  const source = fs.readFileSync(path.join(ROUTES_DIR, file), 'utf8');
  const statements = sqlStatements(source);

  check(`${file}: has at least one legal_answers statement`, statements.length > 0);

  statements.forEach((sql, i) => {
    const firstLine = sql.trim().split('\n')[0].slice(0, 60);

    // An INSERT establishes ownership rather than checking it: the row does not
    // exist yet, so there is nothing to leak. What matters is that the owning
    // column is written from the authenticated caller, never from input.
    if (/^\s*INSERT\s+INTO\s+legal_answers/i.test(sql)) {
      check(
        `${file}: statement ${i + 1} (INSERT) sets lawyer_user_id from the caller`,
        /lawyer_user_id/i.test(sql),
        `INSERT does not name lawyer_user_id: ${firstLine}`
      );
      return;
    }

    const constrained = OWNERSHIP_PREDICATES.some((re) => re.test(sql));
    check(
      `${file}: statement ${i + 1} is pinned to the caller`,
      constrained,
      constrained ? '' : `unconstrained: ${firstLine}`
    );
  });
}

// The deliberately-absent route. Its existence would add the whole IDOR surface
// for zero capability, so its absence is a property worth asserting.
const allSource = files
  .map((f) => fs.readFileSync(path.join(ROUTES_DIR, f), 'utf8'))
  .join('\n');

check(
  'no route is registered at /answers/:id',
  !/router\.(get|all)\(\s*['"`]\/answers\/:/.test(allSource),
  'an id-keyed answer read endpoint was added'
);

// answer_count and asker identity must never reach a lawyer-facing RESPONSE.
//
// Both may legitimately appear in the file: answer_count is maintained by
// UPDATEs there, and asker_user_id is read server-side to address the asker's
// notification. What must not happen is either reaching the JSON the lawyer
// receives, so the checks below look at response construction, not mere
// presence.
const lawyerSource = fs.readFileSync(path.join(ROUTES_DIR, 'lawyerQuestions.js'), 'utf8');

check(
  'lawyerQuestions.js never puts answerCount in a response',
  !/answerCount\s*:/.test(lawyerSource),
  'answerCount appears as a response field'
);
check(
  'lawyerQuestions.js never SELECTs answer_count',
  !/SELECT[\s\S]{0,400}?\banswer_count\b/i.test(lawyerSource),
  'answer_count appears in a SELECT list'
);
check(
  'lawyerQuestions.js never puts asker identity in a response',
  !/askerUserId\s*:|askerName\s*:/.test(lawyerSource),
  'asker identity appears as a response field'
);
/**
 * Extract the argument of each res.json(...) call by matching brackets, so the
 * payload is bounded exactly. A regex cannot do this: `[\s\S]*?` to a closing
 * brace runs past the call and swallows unrelated code, which produced false
 * positives on error responses that merely sit near an UPDATE.
 */
function responsePayloads(source) {
  const payloads = [];
  const re = /res\.(?:status\(\d+\)\.)?json\(/g;
  let m;
  while ((m = re.exec(source)) !== null) {
    let depth = 1;
    let i = m.index + m[0].length;
    const start = i;
    while (i < source.length && depth > 0) {
      const c = source[i];
      if (c === '(' || c === '{' || c === '[') depth += 1;
      else if (c === ')' || c === '}' || c === ']') depth -= 1;
      i += 1;
    }
    payloads.push(source.slice(start, i - 1));
  }
  return payloads;
}

const payloads = responsePayloads(lawyerSource);
const offending = payloads.filter(
  (p) => /asker/i.test(p) || /answer_count|answerCount/i.test(p)
);

check(
  'every lawyer-facing res.json payload omits asker identity and answer counts',
  offending.length === 0,
  offending.length ? `offending payload: ${offending[0].slice(0, 80)}` : ''
);
check(
  'the res.json matcher found the expected payloads (sanity check)',
  payloads.length >= 5,
  `found only ${payloads.length}, so the check above may prove nothing`
);

console.log(`\n${passed} passed, ${failed} failed\n`);
if (failures.length) {
  console.log('FAILURES:');
  failures.forEach((f) => console.log('  - ' + f));
}
process.exit(failed === 0 ? 0 : 1);
