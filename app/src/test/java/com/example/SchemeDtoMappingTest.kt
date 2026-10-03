package com.example

import com.example.data.model.CriterionConditionType
import com.example.data.remote.SchemeCriterionDto
import com.example.data.remote.SchemeDocumentDto
import com.example.data.remote.SchemeDto
import com.example.data.remote.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the wire-to-domain conversion for the server-served scheme catalog.
 *
 * The backend enforces target types with a CHECK constraint, but the client
 * must not depend on that alone: an older or misconfigured server could still
 * send a wrong type, and EligibilityEngine turns a wrong boolean type into a
 * silently inverted rule rather than an error.
 */
class SchemeDtoMappingTest {

    private fun criterionDto(
        conditionType: String,
        targetValue: Any?,
        id: String = "c1"
    ) = SchemeCriterionDto(
        id = id,
        title = "Test",
        conditionType = conditionType,
        targetValue = targetValue,
        requirementDisplay = "req",
        explanationNote = "note",
        whyWeAskReason = "why"
    )

    private fun schemeDto(
        criteria: List<SchemeCriterionDto> = emptyList(),
        category: String = "WELFARE"
    ) = SchemeDto(
        id = "s1",
        name = "Test Scheme",
        shortName = "Test",
        tamilName = "TA",
        hindiName = "HI",
        category = category,
        department = "Dept",
        description = "desc",
        benefitHighlight = "benefit",
        detailedBenefits = listOf("b1"),
        criteria = criteria,
        requiredDocuments = listOf(
            SchemeDocumentDto("d1", "Aadhaar Card", true, "Application Submission", "tip")
        ),
        officialSourceLabel = "src",
        sourceUrl = "https://example.gov.in",
        lastVerifiedDate = "August 2026",
        applicationMethod = "Online",
        applicationSteps = listOf("step1")
    )

    @Test
    fun wellFormedSchemeConvertsFully() {
        val scheme = schemeDto(
            listOf(
                criterionDto("MIN_AGE", 18.0, "a"),
                criterionDto("STUDENT_STATUS", true, "b"),
                criterionDto("STATE_MATCH", "Tamil Nadu", "c"),
                criterionDto("EDUCATION_LEVEL_IN", listOf("Undergraduate", "Diploma"), "d")
            )
        ).toDomain()

        assertNotNull(scheme)
        assertEquals(4, scheme!!.criteria.size)
        assertEquals(1, scheme.requiredDocuments.size)
        assertEquals("d1", scheme.requiredDocuments.first().id)
    }

    @Test
    fun jsonNumbersNarrowToLongForNumericConditions() {
        // JSON has a single number type and Moshi surfaces it as Double. The
        // engine compares with `as? Number`, so a Double would still work, but
        // narrowing keeps display strings identical to the bundled catalog.
        val scheme = schemeDto(listOf(criterionDto("MAX_INCOME", 250000.0))).toDomain()
        val target = scheme!!.criteria.first().targetValue
        assertTrue("Expected a Long, got ${target::class.simpleName}", target is Long)
        assertEquals(250000L, target)
    }

    @Test
    fun stringifiedBooleanIsDroppedRatherThanInverted() {
        // The central safety property. EligibilityEngine reads boolean targets
        // as `targetValue as? Boolean ?: true`, so a string "false" would
        // silently become `true` and invert the rule. Dropping the criterion
        // yields "more information needed" instead of a confident wrong answer.
        val scheme = schemeDto(listOf(criterionDto("STUDENT_STATUS", "false"))).toDomain()
        assertNotNull(scheme)
        assertTrue(
            "A stringified boolean target must not survive conversion.",
            scheme!!.criteria.isEmpty()
        )
    }

    @Test
    fun wrongTypeForNumericConditionIsDropped() {
        val scheme = schemeDto(listOf(criterionDto("MAX_INCOME", "250000"))).toDomain()
        assertTrue(scheme!!.criteria.isEmpty())
    }

    @Test
    fun nullTargetIsDropped() {
        val scheme = schemeDto(listOf(criterionDto("MIN_AGE", null))).toDomain()
        assertTrue(scheme!!.criteria.isEmpty())
    }

    @Test
    fun unknownConditionTypeIsDroppedWithoutLosingTheScheme() {
        // A server that adds a condition type this build does not know about
        // must not break the whole catalog.
        val scheme = schemeDto(
            listOf(
                criterionDto("SOME_FUTURE_CONDITION", true, "future"),
                criterionDto("MIN_AGE", 18.0, "known")
            )
        ).toDomain()

        assertNotNull(scheme)
        assertEquals(1, scheme!!.criteria.size)
        assertEquals(CriterionConditionType.MIN_AGE, scheme.criteria.first().conditionType)
    }

    @Test
    fun unknownCategoryDropsOnlyThatScheme() {
        assertNull(schemeDto(category = "NOT_A_REAL_CATEGORY").toDomain())
        assertNotNull(schemeDto(category = "EDUCATION").toDomain())
    }

    @Test
    fun validBooleanTargetsSurviveBothWays() {
        val isTrue = schemeDto(listOf(criterionDto("FARMER_STATUS", true))).toDomain()
        assertEquals(true, isTrue!!.criteria.first().targetValue)

        val isFalse = schemeDto(listOf(criterionDto("EMPLOYED_STATUS", false))).toDomain()
        assertEquals(false, isFalse!!.criteria.first().targetValue)
    }
}
