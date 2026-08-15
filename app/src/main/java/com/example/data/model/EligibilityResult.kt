package com.example.data.model

enum class EligibilityStatus(val label: String, val shortDesc: String) {
    LIKELY_ELIGIBLE(
        "Likely eligible",
        "Based on the available criteria, you meet the requirements."
    ),
    MORE_INFO_NEEDED(
        "More information needed",
        "We need answers to a few more questions to confirm eligibility."
    ),
    NOT_ELIGIBLE(
        "Doesn't currently match",
        "One or more required conditions do not match your profile."
    )
}

enum class CriterionStatus {
    PASSED,
    FAILED,
    MISSING_INFO
}

data class CriterionEvaluation(
    val criterion: EligibilityCriterion,
    val status: CriterionStatus,
    val userValueDisplay: String,
    val failureReason: String? = null
)

data class SchemeMatchResult(
    val scheme: Scheme,
    val status: EligibilityStatus,
    val criteriaResults: List<CriterionEvaluation>,
    val passedCount: Int,
    val totalEvaluatedCount: Int,
    val missingCriteria: List<CriterionEvaluation>,
    val failedCriteria: List<CriterionEvaluation>,
    val requiredDocuments: List<SchemeDocument>,
    val missingDocuments: List<SchemeDocument>,
    val readyDocuments: List<SchemeDocument>,
    val ruleSummary: String
) {
    val isLikelyEligible: Boolean
        get() = status == EligibilityStatus.LIKELY_ELIGIBLE

    val hasMissingInfo: Boolean
        get() = status == EligibilityStatus.MORE_INFO_NEEDED
}

data class DashboardSummary(
    val totalSchemesCount: Int,
    val likelyEligibleCount: Int,
    val moreInfoNeededCount: Int,
    val notEligibleCount: Int,
    val missingDocumentsCount: Int
)
