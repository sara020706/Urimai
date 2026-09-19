const express = require('express');
const pool = require('../db/pool');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

function toApiProfile(row) {
  return {
    name: row.name,
    age: row.age,
    gender: row.gender,
    state: row.state,
    district: row.district,
    occupation: row.occupation,
    education: row.education,
    annualIncome: row.annual_income !== null ? Number(row.annual_income) : null,
    familySize: row.family_size,
    isStudent: row.is_student,
    isEmployed: row.is_employed,
    isFarmer: row.is_farmer,
    isBusinessOwner: row.is_business_owner,
    socialCategory: row.social_category,
    disabilityStatus: row.disability_status,
    maritalStatus: row.marital_status,
    ownedDocuments: row.owned_documents || []
  };
}

router.get('/', requireAuth, async (req, res) => {
  const result = await pool.query('SELECT * FROM user_profiles WHERE user_id = $1', [req.userId]);
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Profile not found.' });
  }
  res.json(toApiProfile(result.rows[0]));
});

router.put('/', requireAuth, async (req, res) => {
  const p = req.body || {};
  const result = await pool.query(
    `UPDATE user_profiles SET
       name = $1, age = $2, gender = $3, state = $4, district = $5,
       occupation = $6, education = $7, annual_income = $8, family_size = $9,
       is_student = $10, is_employed = $11, is_farmer = $12, is_business_owner = $13,
       social_category = $14, disability_status = $15, marital_status = $16,
       owned_documents = $17, updated_at = now()
     WHERE user_id = $18
     RETURNING *`,
    [
      p.name ?? 'Citizen',
      p.age ?? null,
      p.gender ?? 'Male',
      p.state ?? 'Tamil Nadu',
      p.district ?? 'Chennai',
      p.occupation ?? 'Student',
      p.education ?? 'Undergraduate',
      p.annualIncome ?? null,
      p.familySize ?? 4,
      p.isStudent ?? null,
      p.isEmployed ?? null,
      p.isFarmer ?? null,
      p.isBusinessOwner ?? null,
      p.socialCategory ?? null,
      p.disabilityStatus ?? null,
      p.maritalStatus ?? null,
      Array.isArray(p.ownedDocuments) ? p.ownedDocuments : [],
      req.userId
    ]
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Profile not found.' });
  }
  res.json(toApiProfile(result.rows[0]));
});

module.exports = router;
