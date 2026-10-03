require('dotenv').config();
const pool = require('./pool');

// ---------------------------------------------------------------------------
// SECTION 1 — Tables. Idempotent: CREATE TABLE IF NOT EXISTS.
//
// NOTE: adding a column to a block below does NOT alter an existing table —
// CREATE TABLE IF NOT EXISTS is a silent no-op when the table is present.
// New columns on existing tables belong in SECTION 2.
// ---------------------------------------------------------------------------
const TABLES_SQL = `
CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  display_name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_profiles (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  name TEXT NOT NULL DEFAULT 'Citizen',
  age INTEGER,
  gender TEXT NOT NULL DEFAULT 'Male',
  state TEXT NOT NULL DEFAULT 'Tamil Nadu',
  district TEXT NOT NULL DEFAULT 'Chennai',
  occupation TEXT NOT NULL DEFAULT 'Student',
  education TEXT NOT NULL DEFAULT 'Undergraduate',
  annual_income BIGINT,
  family_size INTEGER NOT NULL DEFAULT 4,
  is_student BOOLEAN,
  is_employed BOOLEAN,
  is_farmer BOOLEAN,
  is_business_owner BOOLEAN,
  social_category TEXT,
  disability_status TEXT,
  marital_status TEXT,
  owned_documents TEXT[] NOT NULL DEFAULT '{}',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS uploaded_documents (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  document_name TEXT NOT NULL,
  file_name TEXT NOT NULL,
  mime_type TEXT,
  file_data BYTEA NOT NULL,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS saved_schemes (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  scheme_id TEXT NOT NULL,
  saved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, scheme_id)
);

-- Guard table for one-shot, non-idempotent steps (seeds, backfills).
CREATE TABLE IF NOT EXISTS schema_migrations (
  name TEXT PRIMARY KEY,
  applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Lawyer professional profile. Created in Phase 0 rather than Phase 2 because
-- loadUser joins it on every authenticated request; the join must not depend on
-- a later phase having been applied.
CREATE TABLE IF NOT EXISTS lawyer_profiles (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  full_name TEXT NOT NULL,
  bar_council_reg_number TEXT NOT NULL,
  bar_council_state TEXT NOT NULL,
  enrollment_year INTEGER,
  years_experience INTEGER NOT NULL DEFAULT 0,
  specializations TEXT[] NOT NULL DEFAULT '{}',
  languages TEXT[] NOT NULL DEFAULT '{}',
  practice_state TEXT,
  practice_district TEXT,
  bio TEXT,
  verification_status TEXT NOT NULL DEFAULT 'PENDING',
  verification_notes TEXT,
  reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
  reviewed_at TIMESTAMPTZ,
  accepting_questions BOOLEAN NOT NULL DEFAULT TRUE,
  contact_email TEXT,
  contact_phone TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS lawyer_verification_documents (
  id BIGSERIAL PRIMARY KEY,
  lawyer_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  doc_type TEXT NOT NULL,
  file_name TEXT NOT NULL,
  mime_type TEXT,
  file_data BYTEA NOT NULL,
  byte_size INTEGER,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Government scheme catalog.
--
-- Hybrid shape: scalar columns for filtering and per-row admin edits, JSONB for
-- the polymorphic criterion target, TEXT[] for ordered plain-string lists.
-- A single JSONB blob per scheme was rejected because editing one criterion
-- would mean read-modify-writing the whole document, and two concurrent admin
-- edits would clobber each other.
CREATE TABLE IF NOT EXISTS schemes (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  short_name TEXT NOT NULL,
  tamil_name TEXT NOT NULL DEFAULT '',
  hindi_name TEXT NOT NULL DEFAULT '',
  category TEXT NOT NULL,
  department TEXT NOT NULL DEFAULT '',
  description TEXT NOT NULL DEFAULT '',
  benefit_highlight TEXT NOT NULL DEFAULT '',
  detailed_benefits TEXT[] NOT NULL DEFAULT '{}',
  official_source_label TEXT NOT NULL DEFAULT 'Official Government Source — Prototype Reference',
  source_url TEXT NOT NULL DEFAULT '',
  last_verified_date TEXT NOT NULL DEFAULT 'August 2026',
  application_method TEXT NOT NULL DEFAULT 'Online via Official Portal / Common Service Center (CSC)',
  application_steps TEXT[] NOT NULL DEFAULT '{}',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INTEGER NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- sort_order exists because criteria and requiredDocuments are ordered Kotlin
-- Lists rendered in authored order; SQL row order is otherwise undefined.
CREATE TABLE IF NOT EXISTS scheme_criteria (
  scheme_id TEXT NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
  id TEXT NOT NULL,
  title TEXT NOT NULL,
  condition_type TEXT NOT NULL,
  target_value JSONB NOT NULL,
  requirement_display TEXT NOT NULL DEFAULT '',
  explanation_note TEXT NOT NULL DEFAULT '',
  why_we_ask_reason TEXT NOT NULL DEFAULT '',
  sort_order INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (scheme_id, id)
);

CREATE TABLE IF NOT EXISTS scheme_documents (
  scheme_id TEXT NOT NULL REFERENCES schemes(id) ON DELETE CASCADE,
  id TEXT NOT NULL,
  name TEXT NOT NULL,
  is_mandatory_for_eligibility BOOLEAN NOT NULL DEFAULT TRUE,
  stage TEXT NOT NULL DEFAULT 'Application Submission',
  tip TEXT NOT NULL DEFAULT 'Keep official digital or physical copy ready',
  sort_order INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (scheme_id, id)
);

-- Anonymous legal questions.
--
-- Note the deliberate omission: there is NO 'anonymous' boolean. Anonymity is
-- not a flag that code must remember to honour — asker_user_id is simply never
-- selected into any lawyer-facing projection. A flag would invite a code path
-- that respects it inconsistently.
CREATE TABLE IF NOT EXISTS legal_questions (
  id BIGSERIAL PRIMARY KEY,
  asker_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  body TEXT NOT NULL,
  category TEXT,
  state TEXT,
  language TEXT NOT NULL DEFAULT 'en',
  status TEXT NOT NULL DEFAULT 'OPEN',
  answer_count INTEGER NOT NULL DEFAULT 0,
  is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
  hidden_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One answer per lawyer per question. This constraint is load-bearing for
-- isolation, not just data hygiene: it means "my answer" is always addressable
-- by (question_id, me), so a lawyer never needs to pass an answer id to read
-- their own work. Edits become UPDATEs. That removes an entire class of IDOR
-- surface rather than guarding it.
CREATE TABLE IF NOT EXISTS legal_answers (
  id BIGSERIAL PRIMARY KEY,
  question_id BIGINT NOT NULL REFERENCES legal_questions(id) ON DELETE CASCADE,
  lawyer_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body TEXT NOT NULL,
  is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
  hidden_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_answer_per_lawyer UNIQUE (question_id, lawyer_user_id)
);

-- Proxied contact between a citizen and a verified lawyer.
--
-- The directory never publishes phone or email. A citizen sends a request; the
-- lawyer accepts, declines or blocks. Contact details are revealed only on
-- acceptance, and only to that one citizen. Publishing a professional's phone
-- number in a searchable directory is a spam vector with no undo.
CREATE TABLE IF NOT EXISTS contact_requests (
  id BIGSERIAL PRIMARY KEY,
  citizen_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  lawyer_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  message TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING',
  responded_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  -- One live request per pair. Re-requesting after a decline is allowed by
  -- clearing the old row, not by stacking a second one.
  CONSTRAINT uq_contact_pair UNIQUE (citizen_user_id, lawyer_user_id)
);

CREATE TABLE IF NOT EXISTS content_reports (
  id BIGSERIAL PRIMARY KEY,
  reporter_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  target_type TEXT NOT NULL,
  target_id BIGINT NOT NULL,
  reason TEXT NOT NULL,
  details TEXT,
  status TEXT NOT NULL DEFAULT 'OPEN',
  resolved_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
  resolved_at TIMESTAMPTZ,
  resolution_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_report_once UNIQUE (reporter_user_id, target_type, target_id)
);

-- In-app notifications, read by polling. Written in the same transaction as the
-- change that triggers them, so there is no outbox to reconcile.
CREATE TABLE IF NOT EXISTS notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  title TEXT NOT NULL,
  body TEXT,
  target_type TEXT,
  target_id BIGINT,
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Audit trail for administrative actions.
-- admin_user_id is ON DELETE RESTRICT on purpose: an audit log that can be
-- erased by deleting the actor is not an audit log.
CREATE TABLE IF NOT EXISTS admin_audit_log (
  id BIGSERIAL PRIMARY KEY,
  admin_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  action TEXT NOT NULL,
  target_type TEXT NOT NULL,
  target_id TEXT,
  reason TEXT,
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
  ip_address TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
`;

// ---------------------------------------------------------------------------
// SECTION 2 — Columns added to pre-existing tables.
//
// ADD COLUMN IF NOT EXISTS is idempotent. NOT NULL DEFAULT is metadata-only on
// PG 11+, so there is no table rewrite and no lock risk.
// ---------------------------------------------------------------------------
const ALTERS_SQL = `
ALTER TABLE users ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status TEXT NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS email TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS status_changed_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS token_valid_from TIMESTAMPTZ NOT NULL DEFAULT now();
`;

// ---------------------------------------------------------------------------
// SECTION 3 — Constraints.
//
// ADD CONSTRAINT has no IF NOT EXISTS, hence the DO block swallowing
// duplicate_object. TEXT + CHECK is used rather than native PG enums:
// CREATE TYPE is not idempotent, and ALTER TYPE ... ADD VALUE cannot run
// inside a transaction block.
// ---------------------------------------------------------------------------
const CONSTRAINTS_SQL = `
DO $$ BEGIN
  ALTER TABLE users ADD CONSTRAINT users_role_chk
    CHECK (role IN ('USER','LAWYER','ADMIN'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE users ADD CONSTRAINT users_account_status_chk
    CHECK (account_status IN ('ACTIVE','SUSPENDED','BLOCKED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE lawyer_profiles ADD CONSTRAINT lawyer_verif_chk
    CHECK (verification_status IN ('PENDING','VERIFIED','REJECTED','SUSPENDED','MORE_INFO_REQUESTED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- SchemeCategory and CriterionConditionType are Kotlin enums. A value outside
-- these sets is not a soft failure in the app, it is a deserialization crash —
-- so an admin must be unable to save one.
DO $$ BEGIN
  ALTER TABLE schemes ADD CONSTRAINT schemes_category_chk CHECK (category IN
    ('ALL','EDUCATION','EMPLOYMENT','FINANCIAL_ASSISTANCE','HOUSING',
     'AGRICULTURE','ENTREPRENEURSHIP','SKILL_DEVELOPMENT','WELFARE'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE legal_questions ADD CONSTRAINT legal_questions_status_chk
    CHECK (status IN ('OPEN','ANSWERED','CLOSED','REMOVED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE contact_requests ADD CONSTRAINT contact_requests_status_chk
    CHECK (status IN ('PENDING','ACCEPTED','DECLINED','BLOCKED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE content_reports ADD CONSTRAINT content_reports_target_chk
    CHECK (target_type IN ('QUESTION','ANSWER','LAWYER'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE content_reports ADD CONSTRAINT content_reports_status_chk
    CHECK (status IN ('OPEN','RESOLVED','DISMISSED'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  ALTER TABLE scheme_criteria ADD CONSTRAINT scheme_criteria_cond_chk CHECK (condition_type IN
    ('MIN_AGE','MAX_AGE','MAX_INCOME','MIN_INCOME','STATE_MATCH','GENDER_MATCH',
     'EDUCATION_LEVEL_IN','STUDENT_STATUS','EMPLOYED_STATUS','FARMER_STATUS',
     'BUSINESS_OWNER_STATUS','SOCIAL_CATEGORY_IN','DISABILITY_STATUS','FAMILY_SIZE_MIN'));
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- The most important constraint in this schema.
--
-- EligibilityEngine reads boolean targets as: targetValue as? Boolean ?: true
-- A boolean stored as the STRING "false" makes that cast return null and the
-- engine silently defaults to true — inverting the rule, with no error anywhere
-- and a plausible-looking wrong verdict as the only symptom.
--
-- Numeric targets are already safe (the engine uses as? Number, covering
-- Int/Long/Double). DISABILITY_STATUS is excluded: the engine ignores its
-- target entirely, and the existing catalog stores the string 'Yes'.
DO $$ BEGIN
  ALTER TABLE scheme_criteria ADD CONSTRAINT scheme_criteria_target_type_chk CHECK (
    CASE
      WHEN condition_type IN ('STUDENT_STATUS','EMPLOYED_STATUS',
                              'FARMER_STATUS','BUSINESS_OWNER_STATUS')
        THEN jsonb_typeof(target_value) = 'boolean'
      WHEN condition_type IN ('MIN_AGE','MAX_AGE','MAX_INCOME',
                              'MIN_INCOME','FAMILY_SIZE_MIN')
        THEN jsonb_typeof(target_value) = 'number'
      WHEN condition_type IN ('EDUCATION_LEVEL_IN','SOCIAL_CATEGORY_IN')
        THEN jsonb_typeof(target_value) IN ('array','string')
      WHEN condition_type IN ('STATE_MATCH','GENDER_MATCH')
        THEN jsonb_typeof(target_value) = 'string'
      ELSE TRUE
    END);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;
`;

// ---------------------------------------------------------------------------
// SECTION 4 — Indexes.
// ---------------------------------------------------------------------------
const INDEXES_SQL = `
CREATE INDEX IF NOT EXISTS idx_users_role_status ON users (role, account_status);
CREATE INDEX IF NOT EXISTS idx_audit_admin ON admin_audit_log (admin_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_target ON admin_audit_log (target_type, target_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lawyer_verif ON lawyer_profiles (verification_status);

-- Directory. The partial index keeps the browse query on the small verified
-- subset; the GIN indexes serve the && overlap filters on the array columns.
CREATE INDEX IF NOT EXISTS idx_lawyer_dir ON lawyer_profiles
  (practice_state, practice_district, years_experience)
  WHERE verification_status = 'VERIFIED';
CREATE INDEX IF NOT EXISTS idx_lawyer_spec_gin ON lawyer_profiles USING GIN (specializations);
CREATE INDEX IF NOT EXISTS idx_lawyer_lang_gin ON lawyer_profiles USING GIN (languages);

CREATE INDEX IF NOT EXISTS idx_contact_lawyer ON contact_requests (lawyer_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_contact_citizen ON contact_requests (citizen_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lawyer_docs_user ON lawyer_verification_documents (lawyer_user_id);

-- Partial: a rejected fraudulent registration must not permanently block the
-- legitimate holder of that bar council number from registering.
CREATE UNIQUE INDEX IF NOT EXISTS uq_lawyer_bar_active
  ON lawyer_profiles (bar_council_state, upper(bar_council_reg_number))
  WHERE verification_status IN ('PENDING','VERIFIED','MORE_INFO_REQUESTED');

CREATE INDEX IF NOT EXISTS idx_schemes_active ON schemes (category, sort_order) WHERE is_active = TRUE;

-- Partial index keeps the unread-count badge poll cheap no matter how large the
-- notifications table grows.
CREATE INDEX IF NOT EXISTS idx_notif_unread ON notifications (user_id, created_at DESC) WHERE read_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications (user_id, created_at DESC);

-- Q&A. The partial indexes matter disproportionately: every hot query carries
-- exactly these predicates, so the qualifying subset stays small as the tables grow.
CREATE INDEX IF NOT EXISTS idx_q_asker ON legal_questions (asker_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_q_feed ON legal_questions (created_at DESC)
  WHERE is_hidden = FALSE AND status IN ('OPEN','ANSWERED');
CREATE INDEX IF NOT EXISTS idx_q_feed_filter ON legal_questions (category, state, language, created_at DESC)
  WHERE is_hidden = FALSE;
CREATE INDEX IF NOT EXISTS idx_a_question ON legal_answers (question_id);
CREATE INDEX IF NOT EXISTS idx_a_lawyer ON legal_answers (lawyer_user_id, created_at DESC);
-- (question_id, lawyer_user_id) is already covered by uq_answer_per_lawyer.

CREATE INDEX IF NOT EXISTS idx_reports_open ON content_reports (created_at DESC) WHERE status = 'OPEN';
CREATE INDEX IF NOT EXISTS idx_reports_target ON content_reports (target_type, target_id);
`;

// Sentinel: a one-shot step returns this to say "not applied, let me run again".
const RETRY_LATER = Symbol('RETRY_LATER');

/**
 * Run `fn` exactly once across all invocations of this migration, ever.
 *
 * The INSERT claims the name atomically, so a concurrent second runner sees
 * rowCount 0 and skips. Use for anything not safely repeatable.
 */
async function once(name, fn) {
  const { rowCount } = await pool.query(
    'INSERT INTO schema_migrations (name) VALUES ($1) ON CONFLICT DO NOTHING',
    [name]
  );
  if (rowCount !== 1) return;

  console.log(`  running one-shot: ${name}`);
  try {
    const result = await fn();
    // A step may report that it did not actually apply (e.g. its target did not
    // exist yet). Release the claim so a later run can retry it.
    if (result === RETRY_LATER) {
      await pool.query('DELETE FROM schema_migrations WHERE name = $1', [name]);
    }
  } catch (err) {
    // Never leave a failed step marked as applied.
    await pool.query('DELETE FROM schema_migrations WHERE name = $1', [name]);
    throw err;
  }
}

/**
 * Promote a bootstrap admin named by env.
 *
 * Deliberately not an endpoint — an admin-signup route is a permanent backdoor.
 * No-op when the env var is unset.
 */
async function seedAdmin() {
  const username = String(process.env.BOOTSTRAP_ADMIN_USERNAME || '').trim().toLowerCase();
  if (!username) {
    console.log('  BOOTSTRAP_ADMIN_USERNAME not set — skipping admin promotion.');
    // Nothing was applied, so do not consume the one-shot claim: setting the
    // env var later must still promote.
    return RETRY_LATER;
  }
  const { rowCount } = await pool.query(
    `UPDATE users SET role = 'ADMIN' WHERE username = $1`,
    [username]
  );
  if (rowCount === 1) {
    console.log(`  promoted "${username}" to ADMIN.`);
    return;
  }
  console.warn(`  WARNING: no user named "${username}" — nothing promoted. Sign that account up, then re-run migrate.`);
  return RETRY_LATER;
}

/**
 * Load the scheme catalog extracted from SchemeRepository.kt.
 *
 * Runs inside one transaction: a partially-seeded catalog would produce wrong
 * eligibility verdicts, which is worse than no catalog at all.
 *
 * Idempotent in its own right (ON CONFLICT upserts), so re-running is safe even
 * though once() normally prevents it.
 */
async function seedSchemes() {
  const fs = require('fs');
  const path = require('path');
  const file = path.join(__dirname, '..', '..', 'tools', 'schemes.json');

  if (!fs.existsSync(file)) {
    console.warn(`  WARNING: ${file} not found — skipping catalog seed.`);
    console.warn('  Generate it with: python backend/tools/extract_schemes.py');
    return RETRY_LATER;
  }

  const schemes = JSON.parse(fs.readFileSync(file, 'utf8'));
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    for (let i = 0; i < schemes.length; i++) {
      const s = schemes[i];
      await client.query(
        `INSERT INTO schemes (
           id, name, short_name, tamil_name, hindi_name, category, department,
           description, benefit_highlight, detailed_benefits,
           official_source_label, source_url, last_verified_date,
           application_method, application_steps, sort_order, updated_at
         ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16, now())
         ON CONFLICT (id) DO UPDATE SET
           name = EXCLUDED.name,
           short_name = EXCLUDED.short_name,
           tamil_name = EXCLUDED.tamil_name,
           hindi_name = EXCLUDED.hindi_name,
           category = EXCLUDED.category,
           department = EXCLUDED.department,
           description = EXCLUDED.description,
           benefit_highlight = EXCLUDED.benefit_highlight,
           detailed_benefits = EXCLUDED.detailed_benefits,
           official_source_label = EXCLUDED.official_source_label,
           source_url = EXCLUDED.source_url,
           last_verified_date = EXCLUDED.last_verified_date,
           application_method = EXCLUDED.application_method,
           application_steps = EXCLUDED.application_steps,
           sort_order = EXCLUDED.sort_order,
           updated_at = now()`,
        [
          s.id, s.name, s.shortName, s.tamilName, s.hindiName, s.category,
          s.department, s.description, s.benefitHighlight, s.detailedBenefits,
          s.officialSourceLabel, s.sourceUrl, s.lastVerifiedDate,
          s.applicationMethod, s.applicationSteps, i
        ]
      );

      // Replace children wholesale so a criterion removed upstream disappears
      // here too; a stale extra rule would silently change verdicts.
      await client.query('DELETE FROM scheme_criteria WHERE scheme_id = $1', [s.id]);
      for (let j = 0; j < s.criteria.length; j++) {
        const c = s.criteria[j];
        await client.query(
          `INSERT INTO scheme_criteria (
             scheme_id, id, title, condition_type, target_value,
             requirement_display, explanation_note, why_we_ask_reason, sort_order
           ) VALUES ($1,$2,$3,$4,$5::jsonb,$6,$7,$8,$9)`,
          [
            s.id, c.id, c.title, c.conditionType,
            // JSON.stringify preserves the JS type: true -> true (not "true"),
            // 250000 -> 250000, ["a","b"] -> ["a","b"]. This is what keeps the
            // polymorphic target round-tripping into Kotlin correctly.
            JSON.stringify(c.targetValue),
            c.requirementDisplay, c.explanationNote, c.whyWeAskReason, j
          ]
        );
      }

      await client.query('DELETE FROM scheme_documents WHERE scheme_id = $1', [s.id]);
      for (let k = 0; k < s.requiredDocuments.length; k++) {
        const d = s.requiredDocuments[k];
        await client.query(
          `INSERT INTO scheme_documents (
             scheme_id, id, name, is_mandatory_for_eligibility, stage, tip, sort_order
           ) VALUES ($1,$2,$3,$4,$5,$6,$7)`,
          [s.id, d.id, d.name, d.isMandatoryForEligibility, d.stage, d.tip, k]
        );
      }
    }

    await client.query('COMMIT');
    const counts = await client.query(
      `SELECT (SELECT count(*) FROM schemes) AS schemes,
              (SELECT count(*) FROM scheme_criteria) AS criteria,
              (SELECT count(*) FROM scheme_documents) AS documents`
    );
    const c = counts.rows[0];
    console.log(`  seeded ${c.schemes} schemes, ${c.criteria} criteria, ${c.documents} documents.`);
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

async function migrate() {
  console.log('Creating tables...');
  await pool.query(TABLES_SQL);

  console.log('Applying column additions...');
  await pool.query(ALTERS_SQL);

  console.log('Applying constraints...');
  await pool.query(CONSTRAINTS_SQL);

  console.log('Creating indexes...');
  await pool.query(INDEXES_SQL);

  console.log('Running one-shot steps...');
  await once('seed_admin_v1', seedAdmin);
  await once('seed_schemes_v1', seedSchemes);

  console.log('Migration complete.');
  await pool.end();
}

migrate().catch((err) => {
  console.error('Migration failed:', err);
  process.exit(1);
});
