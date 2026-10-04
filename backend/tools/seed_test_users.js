/**
 * Create one test account per role and verification state.
 *
 * Intended for development only. Every account uses a shared, well-known
 * password and a `test_` prefix so these are obviously not real users and can
 * be removed in one query.
 *
 *   node tools/seed_test_users.js          create or reset the accounts
 *   node tools/seed_test_users.js --remove delete them
 *
 * Safety: refuses to run unless ALLOW_TEST_SEED=true, so it cannot be executed
 * against a production database by reflex. These are known-password accounts
 * with admin rights; seeding them where real users exist would be a serious
 * hole.
 */

require('dotenv').config();
const bcrypt = require('bcryptjs');
const pool = require('./../src/db/pool');

const PASSWORD = 'Test@1234';
const PREFIX = 'test_';

const ACCOUNTS = [
  {
    username: `${PREFIX}citizen`,
    displayName: 'Test Citizen',
    role: 'USER',
    accountStatus: 'ACTIVE',
    note: 'Ordinary citizen. Browse schemes, ask questions, contact lawyers.'
  },
  {
    username: `${PREFIX}citizen2`,
    displayName: 'Second Citizen',
    role: 'USER',
    accountStatus: 'ACTIVE',
    note: 'Second citizen, for checking one user cannot see another\'s question.'
  },
  {
    username: `${PREFIX}suspended`,
    displayName: 'Suspended Citizen',
    role: 'USER',
    accountStatus: 'SUSPENDED',
    statusReason: 'Suspended for testing. Sign-in should be refused.',
    note: 'Login must fail with ACCOUNT_SUSPENDED.'
  },
  {
    username: `${PREFIX}lawyer`,
    displayName: 'Adv. Verified',
    role: 'LAWYER',
    accountStatus: 'ACTIVE',
    lawyer: {
      fullName: 'Adv. Meena Verified',
      regNumber: 'TN/TEST/0001',
      state: 'Tamil Nadu',
      district: 'Chennai',
      years: 12,
      specializations: ['Welfare Law', 'Consumer'],
      languages: ['Tamil', 'English'],
      status: 'VERIFIED',
      email: 'verified.lawyer@example.test',
      phone: '+91-90000-00001',
      bio: 'Test account. Practises welfare and consumer law.'
    },
    note: 'Approved lawyer. Can answer questions and appears in the directory.'
  },
  {
    username: `${PREFIX}lawyer2`,
    displayName: 'Adv. Second',
    role: 'LAWYER',
    accountStatus: 'ACTIVE',
    lawyer: {
      fullName: 'Adv. Ravi Second',
      regNumber: 'TN/TEST/0002',
      state: 'Tamil Nadu',
      district: 'Madurai',
      years: 7,
      specializations: ['Family', 'Property'],
      languages: ['Tamil'],
      status: 'VERIFIED',
      email: 'second.lawyer@example.test',
      phone: '+91-90000-00002',
      bio: 'Test account. Second verified lawyer, for answer-isolation checks.'
    },
    note: 'Second approved lawyer. Use with test_lawyer to check answer isolation.'
  },
  {
    username: `${PREFIX}pending`,
    displayName: 'Adv. Pending',
    role: 'LAWYER',
    accountStatus: 'ACTIVE',
    lawyer: {
      fullName: 'Adv. Priya Pending',
      regNumber: 'TN/TEST/0003',
      state: 'Tamil Nadu',
      district: 'Coimbatore',
      years: 3,
      specializations: ['Employment'],
      languages: ['Tamil', 'English'],
      status: 'PENDING',
      email: 'pending.lawyer@example.test',
      phone: '+91-90000-00003',
      bio: 'Test account. Awaiting verification.'
    },
    note: 'Unapproved lawyer. Lawyer features must be refused (LAWYER_NOT_VERIFIED).'
  },
  {
    username: `${PREFIX}admin`,
    displayName: 'Test Administrator',
    role: 'ADMIN',
    accountStatus: 'ACTIVE',
    note: 'Administrator. Review lawyers, manage users, moderate, read the audit log.'
  }
];

const USERNAMES = ACCOUNTS.map((a) => a.username);

async function remove() {
  const { rows } = await pool.query(
    'SELECT id, username FROM users WHERE username = ANY($1)',
    [USERNAMES]
  );
  if (rows.length === 0) {
    console.log('No test accounts found.');
    return;
  }
  const ids = rows.map((r) => r.id);
  // admin_audit_log is ON DELETE RESTRICT, so its rows must go first.
  await pool.query('DELETE FROM admin_audit_log WHERE admin_user_id = ANY($1)', [ids]);
  const deleted = await pool.query(
    'DELETE FROM users WHERE id = ANY($1) RETURNING username',
    [ids]
  );
  console.log('Removed:', deleted.rows.map((r) => r.username).join(', '));
}

async function seed() {
  const passwordHash = await bcrypt.hash(PASSWORD, 10);
  const client = await pool.connect();

  try {
    await client.query('BEGIN');

    for (const account of ACCOUNTS) {
      const inserted = await client.query(
        `INSERT INTO users (username, password_hash, display_name, role, account_status, status_reason)
         VALUES ($1,$2,$3,$4,$5,$6)
         ON CONFLICT (username) DO UPDATE SET
           password_hash = EXCLUDED.password_hash,
           display_name = EXCLUDED.display_name,
           role = EXCLUDED.role,
           account_status = EXCLUDED.account_status,
           status_reason = EXCLUDED.status_reason
         RETURNING id`,
        [
          account.username,
          passwordHash,
          account.displayName,
          account.role,
          account.accountStatus,
          account.statusReason || null
        ]
      );
      const userId = inserted.rows[0].id;

      // Signup normally creates this; the seed inserts directly, so do it here.
      await client.query(
        `INSERT INTO user_profiles (user_id, name) VALUES ($1,$2)
         ON CONFLICT (user_id) DO NOTHING`,
        [userId, account.displayName]
      );

      if (account.lawyer) {
        const l = account.lawyer;
        await client.query(
          `INSERT INTO lawyer_profiles (
             user_id, full_name, bar_council_reg_number, bar_council_state,
             years_experience, specializations, languages, practice_state,
             practice_district, bio, verification_status, contact_email,
             contact_phone, accepting_questions
           ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,TRUE)
           ON CONFLICT (user_id) DO UPDATE SET
             full_name = EXCLUDED.full_name,
             bar_council_reg_number = EXCLUDED.bar_council_reg_number,
             bar_council_state = EXCLUDED.bar_council_state,
             years_experience = EXCLUDED.years_experience,
             specializations = EXCLUDED.specializations,
             languages = EXCLUDED.languages,
             practice_state = EXCLUDED.practice_state,
             practice_district = EXCLUDED.practice_district,
             bio = EXCLUDED.bio,
             verification_status = EXCLUDED.verification_status,
             contact_email = EXCLUDED.contact_email,
             contact_phone = EXCLUDED.contact_phone,
             accepting_questions = TRUE`,
          [
            userId, l.fullName, l.regNumber, l.state, l.years,
            l.specializations, l.languages, l.state, l.district, l.bio,
            l.status, l.email, l.phone
          ]
        );
      }
    }

    await client.query('COMMIT');
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }

  console.log('');
  console.log('  Test accounts ready. Password for all: ' + PASSWORD);
  console.log('');
  console.log('  ' + 'USERNAME'.padEnd(18) + 'ROLE'.padEnd(8) + 'STATE');
  console.log('  ' + '-'.repeat(70));
  for (const a of ACCOUNTS) {
    const state = a.lawyer ? a.lawyer.status : a.accountStatus;
    console.log('  ' + a.username.padEnd(18) + a.role.padEnd(8) + state);
    console.log('  ' + ' '.repeat(26) + a.note);
  }
  console.log('');
}

async function main() {
  if (String(process.env.ALLOW_TEST_SEED || '') !== 'true') {
    console.error('Refusing to run without ALLOW_TEST_SEED=true.');
    console.error('These are known-password accounts including an administrator;');
    console.error('seeding them anywhere real would be a serious hole.');
    process.exit(1);
  }

  if (process.argv.includes('--remove')) {
    await remove();
  } else {
    await seed();
  }
  await pool.end();
}

main().catch((err) => {
  console.error('Failed:', err.message);
  process.exit(1);
});
