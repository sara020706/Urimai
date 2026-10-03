package com.example.data.remote

import com.example.data.model.CriterionConditionType
import com.example.data.model.EligibilityCriterion
import com.example.data.model.Scheme
import com.example.data.model.SchemeCategory
import com.example.data.model.SchemeDocument
import com.squareup.moshi.JsonClass

/**
 * Wire types for the scheme catalog served by GET /schemes.
 *
 * These are deliberately separate from the domain model in data/model: the
 * domain types use Kotlin enums, and an unknown enum value arriving from the
 * server would throw during deserialization. Parsing into a DTO first lets
 * [toDomain] drop an unrecognised scheme instead of crashing the whole catalog.
 */
@JsonClass(generateAdapter = true)
data class SchemeCriterionDto(
    val id: String,
    val title: String,
    val conditionType: String,
    /**
     * Polymorphic by design: Boolean, Number or List<String> depending on
     * conditionType. Moshi maps a JSON boolean to Boolean, a number to Double,
     * and an array to List<Any?>.
     *
     * EligibilityEngine reads this as `as? Number` / `as? Boolean` / `is List<*>`,
     * so the JSON type must be preserved exactly. The backend enforces that with
     * a CHECK constraint on scheme_criteria.target_value; [toDomain] re-checks it
     * here rather than trusting the server, because a boolean that arrives as a
     * string silently inverts the rule instead of failing.
     */
    val targetValue: Any?,
    val requirementDisplay: String,
    val explanationNote: String,
    val whyWeAskReason: String
)

@JsonClass(generateAdapter = true)
data class SchemeDocumentDto(
    val id: String,
    val name: String,
    val isMandatoryForEligibility: Boolean,
    val stage: String,
    val tip: String
)

@JsonClass(generateAdapter = true)
data class SchemeDto(
    val id: String,
    val name: String,
    val shortName: String,
    val tamilName: String,
    val hindiName: String,
    val category: String,
    val department: String,
    val description: String,
    val benefitHighlight: String,
    val detailedBenefits: List<String>,
    val criteria: List<SchemeCriterionDto>,
    val requiredDocuments: List<SchemeDocumentDto>,
    val officialSourceLabel: String,
    val sourceUrl: String,
    val lastVerifiedDate: String,
    val applicationMethod: String,
    val applicationSteps: List<String>
)

/** Condition types whose target must be a real Boolean. */
private val BOOLEAN_CONDITIONS = setOf(
    CriterionConditionType.STUDENT_STATUS,
    CriterionConditionType.EMPLOYED_STATUS,
    CriterionConditionType.FARMER_STATUS,
    CriterionConditionType.BUSINESS_OWNER_STATUS
)

/** Condition types whose target must be numeric. */
private val NUMERIC_CONDITIONS = setOf(
    CriterionConditionType.MIN_AGE,
    CriterionConditionType.MAX_AGE,
    CriterionConditionType.MAX_INCOME,
    CriterionConditionType.MIN_INCOME,
    CriterionConditionType.FAMILY_SIZE_MIN
)

/**
 * Convert a criterion, or return null if it cannot be trusted.
 *
 * A criterion with the wrong target type is dropped rather than evaluated.
 * Dropping it makes the scheme report "more information needed"; keeping it
 * would let EligibilityEngine's `as? Boolean ?: true` fall back to `true` and
 * silently invert the rule — a wrong verdict that looks entirely plausible.
 */
private fun SchemeCriterionDto.toDomain(): EligibilityCriterion? {
    val condition = CriterionConditionType.entries.firstOrNull { it.name == conditionType }
        ?: return null

    val target: Any = when {
        targetValue == null -> return null

        condition in BOOLEAN_CONDITIONS -> targetValue as? Boolean ?: return null

        condition in NUMERIC_CONDITIONS -> {
            // JSON has one number type; Moshi surfaces it as Double. Narrow to
            // Long so the engine's `as? Number` comparisons behave as they do
            // with the hardcoded catalog.
            val number = targetValue as? Number ?: return null
            number.toLong()
        }

        targetValue is List<*> -> targetValue.mapNotNull { it as? String }

        else -> targetValue.toString()
    }

    return EligibilityCriterion(
        id = id,
        title = title,
        conditionType = condition,
        targetValue = target,
        requirementDisplay = requirementDisplay,
        explanationNote = explanationNote,
        whyWeAskReason = whyWeAskReason
    )
}

private fun SchemeDocumentDto.toDomain() = SchemeDocument(
    id = id,
    name = name,
    isMandatoryForEligibility = isMandatoryForEligibility,
    stage = stage,
    tip = tip
)

/**
 * Convert to the domain model, or null if the scheme cannot be represented.
 *
 * Returning null for an unknown category drops one scheme rather than failing
 * the entire catalog load, which would leave the user with nothing.
 */
fun SchemeDto.toDomain(): Scheme? {
    val schemeCategory = SchemeCategory.entries.firstOrNull { it.name == category } ?: return null
    return Scheme(
        id = id,
        name = name,
        shortName = shortName,
        tamilName = tamilName,
        hindiName = hindiName,
        category = schemeCategory,
        department = department,
        description = description,
        benefitHighlight = benefitHighlight,
        detailedBenefits = detailedBenefits,
        criteria = criteria.mapNotNull { it.toDomain() },
        requiredDocuments = requiredDocuments.map { it.toDomain() },
        officialSourceLabel = officialSourceLabel,
        sourceUrl = sourceUrl,
        lastVerifiedDate = lastVerifiedDate,
        applicationMethod = applicationMethod,
        applicationSteps = applicationSteps
    )
}
