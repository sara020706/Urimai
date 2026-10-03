package com.example

import com.example.data.model.CriterionConditionType
import com.example.data.model.CriterionStatus
import com.example.data.model.EligibilityCriterion
import com.example.data.model.EligibilityStatus
import com.example.data.model.Scheme
import com.example.data.model.SchemeCategory
import com.example.data.model.SchemeDocument
import com.example.data.model.UserProfile
import com.example.engine.EligibilityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the deterministic eligibility engine.
 *
 * The engine is the product's core guarantee - same profile plus same scheme
 * always yields the same verdict - so it is tested directly, with no Android
 * framework, no network and no AI involved.
 */
class EligibilityEngineTest {

    private fun criterion(
        type: CriterionConditionType,
        target: Any,
        id: String = "c1"
    ) = EligibilityCriterion(
        id = id,
        title = "Test criterion",
        conditionType = type,
        targetValue = target,
        requirementDisplay = "Test requirement",
        explanationNote = "Test note",
        whyWeAskReason = "Test reason"
    )

    private fun scheme(
        vararg criteria: EligibilityCriterion,
        documents: List<SchemeDocument> = emptyList()
    ) = Scheme(
        id = "test_scheme",
        name = "Test Scheme",
        shortName = "Test",
        tamilName = "Test TA",
        hindiName = "Test HI",
        category = SchemeCategory.WELFARE,
        department = "Test Department",
        description = "A scheme used only in tests.",
        benefitHighlight = "Test benefit",
        detailedBenefits = listOf("Benefit one"),
        criteria = criteria.toList(),
        requiredDocuments = documents,
        sourceUrl = "https://example.gov.in"
    )

    // --- Status aggregation ------------------------------------------------

    @Test
    fun allCriteriaPassingYieldsLikelyEligible() {
        val profile = UserProfile(age = 25, annualIncome = 100000L, state = "Tamil Nadu")
        val result = EligibilityEngine.evaluateScheme(
            profile,
            scheme(
                criterion(CriterionConditionType.MIN_AGE, 18, "age"),
                criterion(CriterionConditionType.MAX_INCOME, 250000L, "income"),
                criterion(CriterionConditionType.STATE_MATCH, "Tamil Nadu", "state")
            )
        )
        assertEquals(EligibilityStatus.LIKELY_ELIGIBLE, result.status)
        assertTrue(result.failedCriteria.isEmpty())
        assertTrue(result.missingCriteria.isEmpty())
    }

    @Test
    fun aFailedCriterionYieldsNotEligible() {
        val profile = UserProfile(age = 25, annualIncome = 500000L)
        val result = EligibilityEngine.evaluateScheme(
            profile,
            scheme(criterion(CriterionConditionType.MAX_INCOME, 250000L))
        )
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status)
        assertEquals(1, result.failedCriteria.size)
    }

    @Test
    fun missingDataWithNoFailuresYieldsMoreInfoNeeded() {
        val profile = UserProfile(age = null)
        val result = EligibilityEngine.evaluateScheme(
            profile,
            scheme(criterion(CriterionConditionType.MIN_AGE, 18))
        )
        assertEquals(EligibilityStatus.MORE_INFO_NEEDED, result.status)
        assertEquals(1, result.missingCriteria.size)
    }

    @Test
    fun failureDominatesMissingInfo() {
        val profile = UserProfile(age = null, annualIncome = 900000L)
        val result = EligibilityEngine.evaluateScheme(
            profile,
            scheme(
                criterion(CriterionConditionType.MIN_AGE, 18, "age"),
                criterion(CriterionConditionType.MAX_INCOME, 250000L, "income")
            )
        )
        assertEquals(EligibilityStatus.NOT_ELIGIBLE, result.status)
    }

    @Test
    fun everyCriterionIsEvaluatedEvenAfterOneFails() {
        // No early exit: the UI shows the complete breakdown, so a citizen can
        // see "7 of 8 passed" rather than losing everything after the first
        // failure.
        val profile = UserProfile(age = 25, annualIncome = 900000L, state = "Tamil Nadu")
        val result = EligibilityEngine.evaluateScheme(
            profile,
            scheme(
                criterion(CriterionConditionType.MAX_INCOME, 250000L, "income"),
                criterion(CriterionConditionType.MIN_AGE, 18, "age"),
                criterion(CriterionConditionType.STATE_MATCH, "Tamil Nadu", "state")
            )
        )
        assertEquals(3, result.criteriaResults.size)
        assertEquals(2, result.criteriaResults.count { it.status == CriterionStatus.PASSED })
    }

    // --- Determinism -------------------------------------------------------

    @Test
    fun sameInputsAlwaysProduceTheSameVerdict() {
        val profile = UserProfile(age = 30, annualIncome = 240000L)
        val s = scheme(
            criterion(CriterionConditionType.MIN_AGE, 18, "age"),
            criterion(CriterionConditionType.MAX_INCOME, 250000L, "income")
        )
        val first = EligibilityEngine.evaluateScheme(profile, s)
        repeat(20) {
            val again = EligibilityEngine.evaluateScheme(profile, s)
            assertEquals(first.status, again.status)
            assertEquals(first.ruleSummary, again.ruleSummary)
        }
    }

    // --- Boundaries --------------------------------------------------------

    @Test
    fun ageAndIncomeBoundariesAreInclusive() {
        val minAge = scheme(criterion(CriterionConditionType.MIN_AGE, 18))
        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(age = 18), minAge).status
        )
        assertEquals(
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(age = 17), minAge).status
        )

        val maxAge = scheme(criterion(CriterionConditionType.MAX_AGE, 35))
        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(age = 35), maxAge).status
        )
        assertEquals(
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(age = 36), maxAge).status
        )

        val maxIncome = scheme(criterion(CriterionConditionType.MAX_INCOME, 250000L))
        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(annualIncome = 250000L), maxIncome).status
        )
        assertEquals(
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(annualIncome = 250001L), maxIncome).status
        )
    }

    @Test
    fun incomeFailureMessageQuantifiesTheShortfall() {
        val result = EligibilityEngine.evaluateScheme(
            UserProfile(annualIncome = 500000L),
            scheme(criterion(CriterionConditionType.MAX_INCOME, 300000L))
        )
        val reason = result.failedCriteria.first().failureReason ?: ""
        // The engine reports the excess, not just a bare "too high".
        assertTrue("Expected the excess to be quantified, got: " + reason, reason.contains("200"))
    }

    // --- Specific condition types -----------------------------------------

    @Test
    fun stateMatchTreatsAllAsNationwide() {
        val nationwide = scheme(criterion(CriterionConditionType.STATE_MATCH, "All"))
        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(state = "Kerala"), nationwide).status
        )
    }

    @Test
    fun genderMatchTreatsPreferNotToSayAsMissingRatherThanMismatch() {
        val result = EligibilityEngine.evaluateScheme(
            UserProfile(gender = "Prefer not to say"),
            scheme(criterion(CriterionConditionType.GENDER_MATCH, "Female"))
        )
        assertEquals(EligibilityStatus.MORE_INFO_NEEDED, result.status)
    }

    @Test
    fun socialCategoryPassesViaCategoryOrViaFemaleGender() {
        // Models Stand-Up India: lending to SC/ST *or* women entrepreneurs.
        val s = scheme(criterion(CriterionConditionType.SOCIAL_CATEGORY_IN, listOf("SC", "ST")))

        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(
                UserProfile(gender = "Male", socialCategory = "SC"), s
            ).status
        )
        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(
                UserProfile(gender = "Female", socialCategory = "General / OBC"), s
            ).status
        )
        assertEquals(
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(
                UserProfile(gender = "Male", socialCategory = "General / OBC"), s
            ).status
        )
    }

    @Test
    fun disabilityStatusPassesForAnyValueOtherThanNo() {
        val s = scheme(criterion(CriterionConditionType.DISABILITY_STATUS, true))
        assertEquals(
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(disabilityStatus = "Yes (Visual)"), s).status
        )
        assertEquals(
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(disabilityStatus = "No"), s).status
        )
        assertEquals(
            EligibilityStatus.MORE_INFO_NEEDED,
            EligibilityEngine.evaluateScheme(UserProfile(disabilityStatus = null), s).status
        )
    }

    @Test
    fun nullBooleanStatusMeansUnknownNotFalse() {
        // The nullable Boolean is three-valued on purpose: null must surface as
        // "we have not asked", never be silently read as "no".
        val s = scheme(criterion(CriterionConditionType.STUDENT_STATUS, true))
        assertEquals(
            EligibilityStatus.MORE_INFO_NEEDED,
            EligibilityEngine.evaluateScheme(UserProfile(isStudent = null), s).status
        )
        assertEquals(
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(UserProfile(isStudent = false), s).status
        )
    }

    // --- Regression guard for the Phase 1 scheme migration -----------------

    @Test
    fun booleanTargetsMustBeRealBooleansNotStrings() {
        // EligibilityEngine reads boolean targets as: targetValue as? Boolean ?: true
        // A string "false" makes that cast return null and the engine silently
        // defaults to true, INVERTING the rule with no error anywhere.
        //
        // This is latent today because the Kotlin catalog supplies real Booleans.
        // It becomes reachable once schemes arrive as JSON from the backend, so
        // this test pins the behaviour that the Postgres CHECK constraint on
        // scheme_criteria.target_value exists to prevent.
        val profile = UserProfile(isStudent = false)

        val correct = scheme(criterion(CriterionConditionType.STUDENT_STATUS, false))
        assertEquals(
            "A real Boolean target must pass for a matching profile.",
            EligibilityStatus.LIKELY_ELIGIBLE,
            EligibilityEngine.evaluateScheme(profile, correct).status
        )

        val stringified = scheme(criterion(CriterionConditionType.STUDENT_STATUS, "false"))
        assertEquals(
            "A stringified boolean silently inverts to true - the backend must " +
                "never emit one. If this assertion ever changes, the CHECK constraint " +
                "on scheme_criteria.target_value can be relaxed.",
            EligibilityStatus.NOT_ELIGIBLE,
            EligibilityEngine.evaluateScheme(profile, stringified).status
        )
    }

    // --- Documents and aggregation ----------------------------------------

    @Test
    fun documentsAreSplitIntoReadyAndMissing() {
        val profile = UserProfile(ownedDocuments = setOf("Aadhaar Card"))
        val result = EligibilityEngine.evaluateScheme(
            profile,
            scheme(
                criterion(CriterionConditionType.MIN_AGE, 18),
                documents = listOf(
                    SchemeDocument(id = "d1", name = "Aadhaar Card"),
                    SchemeDocument(id = "d2", name = "Income Certificate")
                )
            )
        )
        assertEquals(1, result.readyDocuments.size)
        assertEquals(1, result.missingDocuments.size)
        assertEquals("Income Certificate", result.missingDocuments.first().name)
    }

    @Test
    fun dashboardSummaryCountsEachStatus() {
        val profile = UserProfile(age = 25, annualIncome = 100000L)
        val results = listOf(
            EligibilityEngine.evaluateScheme(profile, scheme(criterion(CriterionConditionType.MIN_AGE, 18))),
            EligibilityEngine.evaluateScheme(profile, scheme(criterion(CriterionConditionType.MIN_AGE, 60))),
            EligibilityEngine.evaluateScheme(
                profile.copy(isFarmer = null),
                scheme(criterion(CriterionConditionType.FARMER_STATUS, true))
            )
        )
        val summary = EligibilityEngine.computeDashboardSummary(results)
        assertEquals(1, summary.likelyEligibleCount)
        assertEquals(1, summary.notEligibleCount)
        assertEquals(1, summary.moreInfoNeededCount)
    }
}
