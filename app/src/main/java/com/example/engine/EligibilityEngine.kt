package com.example.engine

import com.example.data.model.*
import java.text.NumberFormat
import java.util.Locale

object EligibilityEngine {

    private val indianCurrencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    fun formatInr(amount: Long): String {
        return try {
            indianCurrencyFormat.format(amount)
        } catch (_: Exception) {
            "₹$amount"
        }
    }

    fun evaluateAll(profile: UserProfile, schemes: List<Scheme>): List<SchemeMatchResult> {
        return schemes.map { evaluateScheme(profile, it) }
    }

    fun evaluateScheme(profile: UserProfile, scheme: Scheme): SchemeMatchResult {
        val criteriaResults = mutableListOf<CriterionEvaluation>()

        for (criterion in scheme.criteria) {
            val eval = evaluateCriterion(profile, criterion)
            criteriaResults.add(eval)
        }

        val passedCount = criteriaResults.count { it.status == CriterionStatus.PASSED }
        val failedCriteria = criteriaResults.filter { it.status == CriterionStatus.FAILED }
        val missingCriteria = criteriaResults.filter { it.status == CriterionStatus.MISSING_INFO }

        val status = when {
            failedCriteria.isNotEmpty() -> EligibilityStatus.NOT_ELIGIBLE
            missingCriteria.isNotEmpty() -> EligibilityStatus.MORE_INFO_NEEDED
            else -> EligibilityStatus.LIKELY_ELIGIBLE
        }

        val readyDocs = scheme.requiredDocuments.filter { doc ->
            profile.ownedDocuments.any { owned -> owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true) }
        }

        val missingDocs = scheme.requiredDocuments.filter { doc ->
            profile.ownedDocuments.none { owned -> owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true) }
        }

        val summary = buildRuleSummary(scheme, status, criteriaResults, missingDocs)

        return SchemeMatchResult(
            scheme = scheme,
            status = status,
            criteriaResults = criteriaResults,
            passedCount = passedCount,
            totalEvaluatedCount = scheme.criteria.size,
            missingCriteria = missingCriteria,
            failedCriteria = failedCriteria,
            requiredDocuments = scheme.requiredDocuments,
            missingDocuments = missingDocs,
            readyDocuments = readyDocs,
            ruleSummary = summary
        )
    }

    private fun evaluateCriterion(profile: UserProfile, criterion: EligibilityCriterion): CriterionEvaluation {
        return when (criterion.conditionType) {
            CriterionConditionType.MIN_AGE -> {
                val target = (criterion.targetValue as? Number)?.toInt() ?: 18
                val userAge = profile.age
                if (userAge == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (userAge >= target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, "$userAge years (Meets min $target)")
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, "$userAge years", "Age $userAge is below minimum requirement of $target years.")
                }
            }

            CriterionConditionType.MAX_AGE -> {
                val target = (criterion.targetValue as? Number)?.toInt() ?: 35
                val userAge = profile.age
                if (userAge == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (userAge <= target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, "$userAge years (Meets max $target)")
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, "$userAge years", "Age $userAge exceeds the maximum allowed age of $target years.")
                }
            }

            CriterionConditionType.MAX_INCOME -> {
                val target = (criterion.targetValue as? Number)?.toLong() ?: 300000L
                val userIncome = profile.annualIncome
                if (userIncome == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (userIncome <= target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, "${formatInr(userIncome)} (Within limit of ${formatInr(target)})")
                } else {
                    val excess = userIncome - target
                    CriterionEvaluation(
                        criterion,
                        CriterionStatus.FAILED,
                        formatInr(userIncome),
                        "Income ${formatInr(userIncome)} exceeds the ceiling limit of ${formatInr(target)} by ${formatInr(excess)}."
                    )
                }
            }

            CriterionConditionType.MIN_INCOME -> {
                val target = (criterion.targetValue as? Number)?.toLong() ?: 0L
                val userIncome = profile.annualIncome
                if (userIncome == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (userIncome >= target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, formatInr(userIncome))
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, formatInr(userIncome), "Income is below minimum requirement.")
                }
            }

            CriterionConditionType.STATE_MATCH -> {
                val target = criterion.targetValue.toString()
                val userState = profile.state
                if (userState.isBlank()) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (userState.equals(target, ignoreCase = true) || target.equals("All", ignoreCase = true)) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, userState)
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, userState, "Scheme is restricted to residents of $target (You: $userState).")
                }
            }

            CriterionConditionType.GENDER_MATCH -> {
                val target = criterion.targetValue.toString()
                val userGender = profile.gender
                if (userGender.isBlank() || userGender.equals("Prefer not to say", ignoreCase = true)) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (userGender.equals(target, ignoreCase = true) || target.equals("Any", ignoreCase = true)) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, userGender)
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, userGender, "Scheme is specifically designated for $target citizens.")
                }
            }

            CriterionConditionType.EDUCATION_LEVEL_IN -> {
                val allowedList = when (val target = criterion.targetValue) {
                    is List<*> -> target.map { it.toString() }
                    else -> listOf(target.toString())
                }
                val userEdu = profile.education
                if (userEdu.isBlank()) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else if (allowedList.any { it.equals(userEdu, ignoreCase = true) }) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, userEdu)
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, userEdu, "Requires education in [${allowedList.joinToString(", ")}] (You: $userEdu).")
                }
            }

            CriterionConditionType.STUDENT_STATUS -> {
                val target = criterion.targetValue as? Boolean ?: true
                val userStudent = profile.isStudent
                if (userStudent == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Unknown")
                } else if (userStudent == target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, if (userStudent) "Currently Enrolled Student" else "Not a student")
                } else {
                    val expected = if (target) "active student" else "non-student"
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, if (userStudent) "Student" else "Non-student", "Must be an $expected.")
                }
            }

            CriterionConditionType.EMPLOYED_STATUS -> {
                val target = criterion.targetValue as? Boolean ?: true
                val userEmployed = profile.isEmployed
                if (userEmployed == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Unknown")
                } else if (userEmployed == target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, if (userEmployed) "Employed" else "Unemployed / Seeking skills")
                } else {
                    val expected = if (target) "Employed" else "Unemployed"
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, if (userEmployed) "Employed" else "Unemployed", "Requirement expected $expected.")
                }
            }

            CriterionConditionType.FARMER_STATUS -> {
                val target = criterion.targetValue as? Boolean ?: true
                val userFarmer = profile.isFarmer
                if (userFarmer == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Unknown")
                } else if (userFarmer == target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, if (userFarmer) "Cultivating Farmer" else "Non-farmer")
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, if (userFarmer) "Farmer" else "Non-farmer", "Requires verified farmer status.")
                }
            }

            CriterionConditionType.BUSINESS_OWNER_STATUS -> {
                val target = criterion.targetValue as? Boolean ?: true
                val userBiz = profile.isBusinessOwner
                if (userBiz == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Unknown")
                } else if (userBiz == target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, if (userBiz) "Entrepreneur / Business Owner" else "Individual")
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, if (userBiz) "Business Owner" else "Not a Business Owner", "Requires active enterprise or registered entrepreneur status.")
                }
            }

            CriterionConditionType.SOCIAL_CATEGORY_IN -> {
                val allowedList = when (val target = criterion.targetValue) {
                    is List<*> -> target.map { it.toString() }
                    else -> listOf(target.toString())
                }
                val userCategory = profile.socialCategory
                val userGender = profile.gender
                // Stand up india allows women OR SC/ST
                val passesViaGender = userGender.equals("Female", ignoreCase = true)
                val passesViaCategory = userCategory != null && allowedList.any { userCategory.contains(it, ignoreCase = true) }

                if (passesViaGender || passesViaCategory) {
                    val reason = if (passesViaGender) "Female Entrepreneur" else "Category: $userCategory"
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, reason)
                } else if (userCategory == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not specified")
                } else {
                    CriterionEvaluation(
                        criterion,
                        CriterionStatus.FAILED,
                        userCategory,
                        "Requires belonging to ${allowedList.joinToString(", ")} or Female entrepreneur category."
                    )
                }
            }

            CriterionConditionType.DISABILITY_STATUS -> {
                val userDisability = profile.disabilityStatus
                val hasDisability = userDisability != null && !userDisability.equals("No", ignoreCase = true)
                if (hasDisability) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, userDisability ?: "Yes")
                } else if (userDisability == null) {
                    CriterionEvaluation(criterion, CriterionStatus.MISSING_INFO, "Not provided")
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, "No registered disability", "Disability welfare benefit requires benchmark disability.")
                }
            }

            CriterionConditionType.FAMILY_SIZE_MIN -> {
                val target = (criterion.targetValue as? Number)?.toInt() ?: 1
                val userSize = profile.familySize
                if (userSize >= target) {
                    CriterionEvaluation(criterion, CriterionStatus.PASSED, "$userSize family members")
                } else {
                    CriterionEvaluation(criterion, CriterionStatus.FAILED, "$userSize members", "Requires minimum household size of $target.")
                }
            }
        }
    }

    private fun buildRuleSummary(
        scheme: Scheme,
        status: EligibilityStatus,
        criteriaResults: List<CriterionEvaluation>,
        missingDocs: List<SchemeDocument>
    ): String {
        return when (status) {
            EligibilityStatus.LIKELY_ELIGIBLE -> {
                val docNote = if (missingDocs.isNotEmpty()) {
                    " You may need ${missingDocs.size} additional document(s) before applying (${missingDocs.joinToString(", ") { it.name }})."
                } else {
                    " You appear to have all essential documents ready."
                }
                "Based on the structured rules for ${scheme.shortName}, all ${criteriaResults.size} eligibility checks passed.$docNote"
            }
            EligibilityStatus.MORE_INFO_NEEDED -> {
                val missingNames = criteriaResults.filter { it.status == CriterionStatus.MISSING_INFO }
                    .joinToString(", ") { it.criterion.title }
                "Eligibility cannot be fully verified yet because we need answers for: $missingNames."
            }
            EligibilityStatus.NOT_ELIGIBLE -> {
                val failedNotes = criteriaResults.filter { it.status == CriterionStatus.FAILED }
                    .joinToString("; ") { "${it.criterion.title}: ${it.failureReason ?: "Condition unmet"}" }
                "Does not currently match listed criteria: $failedNotes"
            }
        }
    }

    fun computeDashboardSummary(results: List<SchemeMatchResult>): DashboardSummary {
        val total = results.size
        val likely = results.count { it.status == EligibilityStatus.LIKELY_ELIGIBLE }
        val moreInfo = results.count { it.status == EligibilityStatus.MORE_INFO_NEEDED }
        val notEligible = results.count { it.status == EligibilityStatus.NOT_ELIGIBLE }
        val missingDocsCount = results
            .filter { it.status == EligibilityStatus.LIKELY_ELIGIBLE }
            .flatMap { it.missingDocuments }
            .distinctBy { it.name }
            .size

        return DashboardSummary(
            totalSchemesCount = total,
            likelyEligibleCount = likely,
            moreInfoNeededCount = moreInfo,
            notEligibleCount = notEligible,
            missingDocumentsCount = missingDocsCount
        )
    }
}
