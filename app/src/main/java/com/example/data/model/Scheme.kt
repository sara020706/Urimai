package com.example.data.model

enum class SchemeCategory(val displayName: String, val iconName: String) {
    ALL("All Schemes", "grid_view"),
    EDUCATION("Education", "school"),
    EMPLOYMENT("Employment", "work"),
    FINANCIAL_ASSISTANCE("Financial Aid", "payments"),
    HOUSING("Housing", "home"),
    AGRICULTURE("Agriculture", "agriculture"),
    ENTREPRENEURSHIP("Entrepreneurship", "storefront"),
    SKILL_DEVELOPMENT("Skill Training", "psychology"),
    WELFARE("Social Welfare", "volunteer_activism")
}

enum class CriterionConditionType {
    MIN_AGE,
    MAX_AGE,
    MAX_INCOME,
    MIN_INCOME,
    STATE_MATCH,
    GENDER_MATCH,
    EDUCATION_LEVEL_IN,
    STUDENT_STATUS,
    EMPLOYED_STATUS,
    FARMER_STATUS,
    BUSINESS_OWNER_STATUS,
    SOCIAL_CATEGORY_IN,
    DISABILITY_STATUS,
    FAMILY_SIZE_MIN
}

data class EligibilityCriterion(
    val id: String,
    val title: String,
    val conditionType: CriterionConditionType,
    val targetValue: Any,
    val requirementDisplay: String,
    val explanationNote: String,
    val whyWeAskReason: String
)

data class SchemeDocument(
    val id: String,
    val name: String,
    val isMandatoryForEligibility: Boolean = true,
    val stage: String = "Application Submission", // "Application Submission" vs "Document Verification"
    val tip: String = "Keep official digital or physical copy ready"
)

data class Scheme(
    val id: String,
    val name: String,
    val shortName: String,
    val tamilName: String,
    val hindiName: String,
    val category: SchemeCategory,
    val department: String,
    val description: String,
    val benefitHighlight: String,
    val detailedBenefits: List<String>,
    val criteria: List<EligibilityCriterion>,
    val requiredDocuments: List<SchemeDocument>,
    val officialSourceLabel: String = "Official Government Source — Prototype Reference",
    val sourceUrl: String,
    val lastVerifiedDate: String = "August 2026",
    val applicationMethod: String = "Online via Official Portal / Common Service Center (CSC)",
    val applicationSteps: List<String> = listOf(
        "Register on the official government portal or visit your nearest Common Service Center (CSC).",
        "Upload/submit mandatory identity credentials and category certificates.",
        "Submit the application and receive your official acknowledgment receipt.",
        "Track application status online using your reference application ID."
    )
)
