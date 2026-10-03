package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.model.*
import com.example.data.remote.CriterionSummary
import com.example.data.remote.ExplainSchemeRequest
import com.example.data.remote.SchemeChatRequest
import com.example.data.repository.AiRepository
import com.example.engine.EligibilityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * AI assistance, with a deterministic fallback behind every call.
 *
 * The Gemini API key is NOT in this app. It used to be injected into
 * BuildConfig by the Secrets Gradle plugin, which shipped it inside every APK
 * where anyone could decompile it. All model calls now go through authenticated
 * backend endpoints that hold the key server-side.
 *
 * The boundary that matters is unchanged: AI never decides eligibility. It is
 * handed an already-computed verdict and asked only to narrate it, so an
 * outage, a hallucination or a rate limit can change the WORDING of an
 * explanation but not who is eligible. Every function below falls back to a
 * deterministic implementation when the backend is unreachable or has no key
 * configured.
 */
object UrimaiAiService {

    private const val TAG = "UrimaiAiService"

    /**
     * Set once at startup so the existing call sites keep their signatures.
     * Without it the service has no way to reach the backend and every call
     * takes its deterministic path — which is a supported mode, not an error.
     */
    @Volatile
    private var repository: AiRepository? = null

    fun initialize(context: Context) {
        if (repository == null) {
            synchronized(this) {
                if (repository == null) {
                    repository = AiRepository(context.applicationContext)
                }
            }
        }
    }

    private fun summarize(items: List<CriterionEvaluation>, withReason: Boolean): List<CriterionSummary> =
        items.map { evaluation ->
            CriterionSummary(
                title = evaluation.criterion.title,
                detail = if (withReason) {
                    evaluation.failureReason ?: evaluation.userValueDisplay
                } else {
                    evaluation.userValueDisplay
                }
            )
        }

    /**
     * Parse free text such as "I'm a 22-year-old student from Chennai, family
     * income 2 lakh" into a profile.
     *
     * Uses the regex parser only. The model path was removed with the API key:
     * this function has no callers today, and routing it through the backend
     * would add an endpoint nothing uses. The parser handles Indian numeric
     * conventions (lakh/crore, rupee prefixes) well enough to stand alone.
     */
    suspend fun extractProfileFromNaturalLanguage(userText: String): UserProfile =
        withContext(Dispatchers.IO) {
            fallbackNaturalLanguageExtraction(userText)
        }

    private fun fallbackNaturalLanguageExtraction(text: String): UserProfile {
        val lower = text.lowercase()

        // Age regex
        var age: Int? = null
        val ageMatcher = Pattern.compile("(\\d{1,2})\\s*(?:years|yr|y/o|yo|year\\s*old|age|aged)?").matcher(lower)
        while (ageMatcher.find()) {
            val num = ageMatcher.group(1)?.toIntOrNull()
            if (num != null && num in 14..95) {
                age = num
                break
            }
        }
        if (age == null) {
            val wordAgeMatcher = Pattern.compile("(?:i'm|i am|age is|age)\\s*(\\d{1,2})").matcher(lower)
            if (wordAgeMatcher.find()) {
                age = wordAgeMatcher.group(1)?.toIntOrNull()
            }
        }
        if (age == null) age = 21

        // Location
        var district = "Chennai"
        var state = "Tamil Nadu"
        when {
            lower.contains("chennai") -> { district = "Chennai"; state = "Tamil Nadu" }
            lower.contains("madurai") -> { district = "Madurai"; state = "Tamil Nadu" }
            lower.contains("coimbatore") -> { district = "Coimbatore"; state = "Tamil Nadu" }
            lower.contains("trichy") || lower.contains("tiruchirappalli") -> { district = "Tiruchirappalli"; state = "Tamil Nadu" }
            lower.contains("salem") -> { district = "Salem"; state = "Tamil Nadu" }
            lower.contains("bengaluru") || lower.contains("bangalore") -> { district = "Bengaluru"; state = "Karnataka" }
            lower.contains("karnataka") -> { district = "Bengaluru"; state = "Karnataka" }
            lower.contains("mumbai") || lower.contains("pune") || lower.contains("maharashtra") -> { district = "Mumbai"; state = "Maharashtra" }
            lower.contains("delhi") -> { district = "New Delhi"; state = "Delhi" }
            lower.contains("hyderabad") || lower.contains("telangana") -> { district = "Hyderabad"; state = "Telangana" }
            lower.contains("kerala") || lower.contains("kochi") -> { district = "Kochi"; state = "Kerala" }
        }

        // Student / Occupation / Education
        var isStudent = lower.contains("student") || lower.contains("studying") || lower.contains("college") || lower.contains("engineering") || lower.contains("undergrad")
        var occupation = if (isStudent) "Student" else "Employed"
        var isFarmer = lower.contains("farmer") || lower.contains("farming") || lower.contains("agriculture") || lower.contains("cultivat")
        if (isFarmer) {
            occupation = "Farmer"
            isStudent = false
        }
        val isBiz = lower.contains("business") || lower.contains("shop") || lower.contains("startup") || lower.contains("entrepreneur")
        if (isBiz) {
            occupation = "Self-Employed"
        }

        var education = "Undergraduate"
        when {
            lower.contains("postgrad") || lower.contains("master") || lower.contains("m.tech") || lower.contains("mba") || lower.contains("msc") -> education = "Postgraduate"
            lower.contains("phd") || lower.contains("doctorate") -> education = "Doctorate"
            lower.contains("diploma") || lower.contains("polytechnic") -> education = "Diploma"
            lower.contains("engineering") || lower.contains("b.tech") || lower.contains("b.e") || lower.contains("undergrad") || lower.contains("bachelor") || lower.contains("b.sc") || lower.contains("b.com") -> education = "Undergraduate"
            lower.contains("12th") || lower.contains("hsc") || lower.contains("higher secondary") -> education = "12th Pass"
            lower.contains("10th") || lower.contains("sslc") -> education = "10th Pass"
        }

        // Income parsing
        var income = 200000L
        val lakhMatcher = Pattern.compile("([\\d.]+)\\s*(?:lakh|lakhs|l|lac|lacs)").matcher(lower)
        if (lakhMatcher.find()) {
            val lakhVal = lakhMatcher.group(1)?.toDoubleOrNull()
            if (lakhVal != null) {
                income = (lakhVal * 100000L).toLong()
            }
        } else {
            val directNumMatcher = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*(\\d{5,7})").matcher(lower)
            if (directNumMatcher.find()) {
                val numVal = directNumMatcher.group(1)?.toLongOrNull()
                if (numVal != null) income = numVal
            }
        }

        // Gender
        var gender = "Male"
        if (lower.contains("woman") || lower.contains("female") || lower.contains("girl") || lower.contains("daughter") || lower.contains("she") || lower.contains("her")) {
            gender = "Female"
        }

        return UserProfile(
            name = if (lower.contains("arun")) "Arun" else if (lower.contains("meena")) "Meena" else "Citizen",
            age = age,
            gender = gender,
            state = state,
            district = district,
            occupation = occupation,
            education = education,
            annualIncome = income,
            familySize = 4,
            isStudent = isStudent,
            isEmployed = !isStudent && !isFarmer && !isBiz,
            isFarmer = isFarmer,
            isBusinessOwner = isBiz,
            socialCategory = "General / OBC",
            disabilityStatus = if (lower.contains("disabled") || lower.contains("disability")) "Yes" else "No",
            maritalStatus = "Single"
        )
    }

    /**
     * Explain an already-computed verdict in plain language.
     *
     * The verdict and its per-criterion breakdown are computed locally by
     * [EligibilityEngine] and sent to the backend purely as material to narrate.
     * Falls back to [buildDeterministicExplanation] whenever the backend is
     * unreachable or has no key configured.
     */
    suspend fun generateSchemeExplanation(
        matchResult: SchemeMatchResult,
        profile: UserProfile,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String = withContext(Dispatchers.IO) {
        val scheme = matchResult.scheme
        val repo = repository

        if (repo != null) {
            val request = ExplainSchemeRequest(
                schemeName = scheme.name,
                status = matchResult.status.label,
                department = scheme.department,
                benefitHighlight = scheme.benefitHighlight,
                passed = summarize(
                    matchResult.criteriaResults.filter { it.status == CriterionStatus.PASSED },
                    withReason = false
                ),
                failed = summarize(matchResult.failedCriteria, withReason = true),
                missing = summarize(matchResult.missingCriteria, withReason = false),
                language = language.code
            )
            val explanation = repo.explainScheme(request)
            if (explanation != null) return@withContext explanation
            Log.i(TAG, "AI explanation unavailable; using deterministic text.")
        }

        buildDeterministicExplanation(matchResult, profile, language)
    }

    private fun buildDeterministicExplanation(
        matchResult: SchemeMatchResult,
        profile: UserProfile,
        language: AppLanguage
    ): String {
        val scheme = matchResult.scheme
        val passList = matchResult.criteriaResults.filter { it.status == CriterionStatus.PASSED }
        val failList = matchResult.criteriaResults.filter { it.status == CriterionStatus.FAILED }
        val missingList = matchResult.criteriaResults.filter { it.status == CriterionStatus.MISSING_INFO }
        val missingDocs = matchResult.missingDocuments

        return when (language) {
            AppLanguage.TAMIL -> {
                when (matchResult.status) {
                    EligibilityStatus.LIKELY_ELIGIBLE -> {
                        "நீங்கள் வழங்கிய தகவல்களின் அடிப்படையில், **${scheme.tamilName.ifBlank { scheme.name }}** திட்டத்தின் முக்கிய தகுதி நிபந்தனைகள் அனைத்தையும் நிறைவு செய்கிறீர்கள்.\n\n" +
                        "• **பொருந்திய தகுதிகள்:** ${passList.joinToString(", ") { it.criterion.title }}\n" +
                        (if (missingDocs.isNotEmpty()) "• **தேவையான ஆவணங்கள்:** விண்ணப்பிக்கும் முன் ${missingDocs.joinToString(", ") { it.name }} தயார் நிலையில் வைத்திருக்கவும்.\n" else "• **ஆவணங்கள்:** தேவையான அடிப்படை ஆவணங்கள் தயாராக உள்ளன.\n") +
                        "\n*குறிப்பு: இந்த முடிவு மாதிரி தரவுகளின் அடிப்படையில் கணக்கிடப்பட்டது. இறுதி தகுதி அரசு விதிகளுக்கு உட்பட்டது.*"
                    }
                    EligibilityStatus.MORE_INFO_NEEDED -> {
                        "இத்திட்டத்திற்கான தகுதியை முழுமையாக உறுதிப்படுத்த சில கூடுதல் தகவல்கள் தேவைப்படுகின்றன.\n\n" +
                        "• **தேவைப்படும் தகவல்:** ${missingList.joinToString(", ") { it.criterion.title }}\n" +
                        "தயவுசெய்து மேலே உள்ள கேள்விகளுக்கு பதிலளிக்கவும்."
                    }
                    EligibilityStatus.NOT_ELIGIBLE -> {
                        "தற்போது உள்ள அரசு வழிகாட்டுதலின்படி, உங்கள் சுயவிவரம் இத்திட்டத்தின் சில நிபந்தனைகளுடன் பொருந்தவில்லை.\n\n" +
                        "• **பொருந்தாத நிபந்தனை:** ${failList.joinToString("; ") { "${it.criterion.title}: ${it.failureReason}" }}\n" +
                        "மற்ற பொருத்தமான திட்டங்களை நீங்கள் ஆராயலாம்."
                    }
                }
            }

            AppLanguage.HINDI -> {
                when (matchResult.status) {
                    EligibilityStatus.LIKELY_ELIGIBLE -> {
                        "आपके द्वारा दी गई जानकारी के आधार पर, आप **${scheme.hindiName.ifBlank { scheme.name }}** के सभी प्रमुख पात्रता मानदंडों को पूरा करते हैं।\n\n" +
                        "• **सफल शर्तें:** ${passList.joinToString(", ") { it.criterion.title }}\n" +
                        (if (missingDocs.isNotEmpty()) "• **आवश्यक दस्तावेज:** आवेदन करने से पहले कृपया ${missingDocs.joinToString(", ") { it.name }} तैयार रखें।\n" else "• **दस्तावेज:** सभी मुख्य दस्तावेज तैयार हैं।\n") +
                        "\n*नोट: यह परिणाम उपलब्ध नियमों पर आधारित है। अंतिम निर्णय सरकारी विभाग द्वारा लिया जाएगा।* "
                    }
                    EligibilityStatus.MORE_INFO_NEEDED -> {
                        "इस योजना की पात्रता सुनिश्चित करने के लिए कुछ अतिरिक्त जानकारी की आवश्यकता है।\n\n" +
                        "• **अधूरी जानकारी:** ${missingList.joinToString(", ") { it.criterion.title }}\n" +
                        "कृपया विवरण पूरा करने के लिए उत्तर दें।"
                    }
                    EligibilityStatus.NOT_ELIGIBLE -> {
                        "वर्तमान मानदंडों के अनुसार, आपकी प्रोफ़ाइल इस योजना की कुछ शर्तों से मेल नहीं खाती है।\n\n" +
                        "• **अपात्रता का कारण:** ${failList.joinToString("; ") { "${it.criterion.title}: ${it.failureReason}" }}\n" +
                        "आप अन्य प्रासंगिक सरकारी योजनाओं की जांच कर सकते हैं।"
                    }
                }
            }

            AppLanguage.ENGLISH -> {
                when (matchResult.status) {
                    EligibilityStatus.LIKELY_ELIGIBLE -> {
                        "Based on the information you provided, you appear to meet the primary eligibility conditions for **${scheme.name}**.\n\n" +
                        "• **Why you match:** Your profile fulfills all ${passList.size} required criteria (${passList.joinToString(", ") { it.criterion.title }}).\n" +
                        (if (missingDocs.isNotEmpty()) "• **Document Readiness:** You may still need ${missingDocs.size} document(s) before formal application: ${missingDocs.joinToString(", ") { it.name }}.\n" else "• **Document Readiness:** All essential documents appear ready.\n") +
                        "• **Benefit:** ${scheme.benefitHighlight}\n\n" +
                        "Proceed to review official instructions through the official portal link below."
                    }
                    EligibilityStatus.MORE_INFO_NEEDED -> {
                        "We cannot fully confirm your eligibility for **${scheme.shortName}** because some required details are missing from your profile.\n\n" +
                        "• **Pending Information:** ${missingList.joinToString(", ") { it.criterion.title }}.\n" +
                        "Tap 'Answer this question' on the card above to complete your profile evaluation immediately."
                    }
                    EligibilityStatus.NOT_ELIGIBLE -> {
                        "Your current profile does not meet the specified requirements for **${scheme.shortName}**.\n\n" +
                        "• **Unmet Requirement(s):**\n" +
                        failList.joinToString("\n") { "  - ${it.criterion.title}: ${it.failureReason ?: it.criterion.requirementDisplay}" } +
                        "\n\nYou may explore alternative schemes under the ${scheme.category.displayName} category."
                    }
                }
            }
        }
    }

    /**
     * Answer a follow-up question, scoped to one scheme.
     *
     * Falls back to the keyword-routed responder when the backend is
     * unreachable or has no key configured.
     */
    suspend fun answerCitizenQuestion(
        question: String,
        matchResult: SchemeMatchResult,
        profile: UserProfile,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String = withContext(Dispatchers.IO) {
        val scheme = matchResult.scheme
        val repo = repository

        if (repo != null) {
            val request = SchemeChatRequest(
                question = question,
                schemeName = scheme.name,
                status = matchResult.status.label,
                department = scheme.department,
                passed = summarize(
                    matchResult.criteriaResults.filter { it.status == CriterionStatus.PASSED },
                    withReason = false
                ),
                failed = summarize(matchResult.failedCriteria, withReason = true),
                documents = scheme.requiredDocuments.map { it.name },
                language = language.code
            )
            val answer = repo.schemeChat(request)
            if (answer != null) return@withContext answer
            Log.i(TAG, "AI chat unavailable; using deterministic answer.")
        }

        fallbackAnswerQuestion(question, matchResult, profile, language)
    }

    private fun fallbackAnswerQuestion(
        question: String,
        matchResult: SchemeMatchResult,
        profile: UserProfile,
        language: AppLanguage
    ): String {
        val lower = question.lowercase()
        val scheme = matchResult.scheme

        if (lower.contains("why am i eligible") || lower.contains("why do i qualify") || lower.contains("why qualify")) {
            val passed = matchResult.criteriaResults.filter { it.status == CriterionStatus.PASSED }
            return "You qualify because your profile meets every listed criterion for ${scheme.shortName}:\n" +
                    passed.joinToString("\n") { "✓ ${it.criterion.title}: ${it.userValueDisplay}" }
        }

        if (lower.contains("why am i not") || lower.contains("why don't i qualify") || lower.contains("why fail")) {
            val failed = matchResult.criteriaResults.filter { it.status == CriterionStatus.FAILED }
            return if (failed.isNotEmpty()) {
                "You currently do not meet the following required condition(s):\n" +
                        failed.joinToString("\n") { "✕ ${it.criterion.title}: ${it.failureReason ?: it.criterion.requirementDisplay}" }
            } else {
                "You haven't failed any conditions, but some information is still pending verification."
            }
        }

        if (lower.contains("document") || lower.contains("documents") || lower.contains("what doc")) {
            val allDocs = scheme.requiredDocuments.joinToString("\n") { "• ${it.name} (${it.stage})" }
            val missing = if (matchResult.missingDocuments.isNotEmpty()) {
                "\n\nDocuments you still need to arrange:\n" + matchResult.missingDocuments.joinToString("\n") { "⚠ ${it.name} — ${it.tip}" }
            } else {
                "\n\nYou already have the primary documents ready!"
            }
            return "Required documents for ${scheme.shortName}:\n$allDocs$missing"
        }

        if (lower.contains("tamil") || lower.contains("தமிழ்")) {
            return buildDeterministicExplanation(matchResult, profile, AppLanguage.TAMIL)
        }

        if (lower.contains("hindi") || lower.contains("हिन्दी")) {
            return buildDeterministicExplanation(matchResult, profile, AppLanguage.HINDI)
        }

        if (lower.contains("benefit") || lower.contains("how much") || lower.contains("money") || lower.contains("paisa")) {
            return "${scheme.shortName} Benefits:\n• ${scheme.benefitHighlight}\n\nDetailed provisions:\n" +
                    scheme.detailedBenefits.joinToString("\n") { "• $it" }
        }

        if (lower.contains("source") || lower.contains("department") || lower.contains("official")) {
            return "This scheme is administered by: ${scheme.department}.\nOfficial source portal: ${scheme.sourceUrl}\nLast verified: ${scheme.lastVerifiedDate}."
        }

        // Generic friendly response based on data
        return "For **${scheme.shortName}**:\n" +
                "• Status: ${matchResult.status.label}\n" +
                "• Department: ${scheme.department}\n" +
                "• Key Benefit: ${scheme.benefitHighlight}\n" +
                "• Required criteria checks: ${matchResult.passedCount} of ${matchResult.totalEvaluatedCount} passed.\n\n" +
                "If you need specific help with documentation or criteria, feel free to ask!"
    }
}
