require('dotenv').config();
const pool = require('./pool');

const SQL = `
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
`;

async function migrate() {
  await pool.query(SQL);
  console.log('Migration complete.');
  await pool.end();
}

migrate().catch((err) => {
  console.error('Migration failed:', err);
  process.exit(1);
});
