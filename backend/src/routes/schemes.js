const crypto = require('crypto');
const express = require('express');
const pool = require('../db/pool');
const { wrap } = require('../lib/async');

const router = express.Router();

/**
 * Field names below are the Kotlin property names from Scheme.kt, not snake_case.
 * The client deserializes this straight into its existing data classes, so the
 * shape must match exactly — including SchemeDocument.id, which is easy to miss.
 *
 * `'targetValue', c.target_value` passes JSONB through unquoted. That is what
 * preserves the polymorphic type (boolean stays boolean, number stays number,
 * array stays array) all the way into EligibilityEngine.
 */
const SELECT_SCHEMES = `
  SELECT jsonb_build_object(
    'id', s.id,
    'name', s.name,
    'shortName', s.short_name,
    'tamilName', s.tamil_name,
    'hindiName', s.hindi_name,
    'category', s.category,
    'department', s.department,
    'description', s.description,
    'benefitHighlight', s.benefit_highlight,
    'detailedBenefits', to_jsonb(s.detailed_benefits),
    'officialSourceLabel', s.official_source_label,
    'sourceUrl', s.source_url,
    'lastVerifiedDate', s.last_verified_date,
    'applicationMethod', s.application_method,
    'applicationSteps', to_jsonb(s.application_steps),
    'criteria', COALESCE((
      SELECT jsonb_agg(jsonb_build_object(
               'id', c.id,
               'title', c.title,
               'conditionType', c.condition_type,
               'targetValue', c.target_value,
               'requirementDisplay', c.requirement_display,
               'explanationNote', c.explanation_note,
               'whyWeAskReason', c.why_we_ask_reason
             ) ORDER BY c.sort_order, c.id)
        FROM scheme_criteria c WHERE c.scheme_id = s.id), '[]'::jsonb),
    'requiredDocuments', COALESCE((
      SELECT jsonb_agg(jsonb_build_object(
               'id', d.id,
               'name', d.name,
               'isMandatoryForEligibility', d.is_mandatory_for_eligibility,
               'stage', d.stage,
               'tip', d.tip
             ) ORDER BY d.sort_order, d.id)
        FROM scheme_documents d WHERE d.scheme_id = s.id), '[]'::jsonb)
  ) AS scheme
  FROM schemes s
`;

/**
 * Public catalog. Unauthenticated on purpose: schemes are public government
 * information, and the app lets people browse before signing up.
 */
router.get('/', wrap(async (req, res) => {
  const result = await pool.query(
    SELECT_SCHEMES + ' WHERE s.is_active = TRUE ORDER BY s.sort_order, s.id'
  );
  const schemes = result.rows.map((r) => r.scheme);

  // The catalog changes rarely and is a few hundred KB, so an ETag saves the
  // client re-downloading it on every cold start.
  const body = JSON.stringify(schemes);
  const etag = '"' + crypto.createHash('sha1').update(body).digest('hex') + '"';
  res.setHeader('ETag', etag);
  res.setHeader('Cache-Control', 'public, max-age=300');
  if (req.headers['if-none-match'] === etag) {
    return res.status(304).end();
  }
  res.type('application/json').send(body);
}));

router.get('/:id', wrap(async (req, res) => {
  const result = await pool.query(
    SELECT_SCHEMES + ' WHERE s.id = $1 AND s.is_active = TRUE',
    [req.params.id]
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Scheme not found.', code: 'NOT_FOUND' });
  }
  res.json(result.rows[0].scheme);
}));

module.exports = router;
