package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.example.engine.EligibilityEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object UrimaiAiService {

    private const val TAG = "UrimaiAiService"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun isKeyConfigured(): Boolean {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            key.isNotBlank() && !key.equals("MY_GEMINI_API_KEY", ignoreCase = true)
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun extractProfileFromNaturalLanguage(userText: String): UserProfile = withContext(Dispatchers.IO) {
        if (isKeyConfigured()) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val systemPrompt = """
                    You are Urimai AI, a civic information assistant for Indian government schemes.
                    Analyze the user's personal description and extract structured profile attributes for Indian citizens.
                    Return ONLY valid JSON matching this schema:
                    {
                      "name": "string (or Citizen if unknown)",
                      "age": integer or null,
                      "gender": "Male" or "Female" or "Other" or "Prefer not to say",
                      "state": "string (e.g. Tamil Nadu, Karnataka, Maharashtra, etc.)",
                      "district": "string (e.g. Chennai, Madurai, Bengaluru, etc.)",
                      "occupation": "Student" or "Employed" or "Self-Employed" or "Unemployed" or "Farmer" or "Homemaker",
                      "education": "Below 10th" or "10th Pass" or "12th Pass" or "Diploma" or "Undergraduate" or "Postgraduate" or "Doctorate",
                      "annualIncome": integer in INR (e.g. 200000) or null,
                      "familySize": integer,
                      "isStudent": boolean or null,
                      "isEmployed": boolean or null,
                      "isFarmer": boolean or null,
                      "isBusinessOwner": boolean or null,
                      "socialCategory": "General" or "OBC" or "SC" or "ST" or "EWS" or null,
                      "disabilityStatus": "No" or "Yes"
                    }
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "Extract citizen profile from this text:\n\"$userText\""))
                            })
                        })
                    }
                    put("contents", contents)
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", systemPrompt))
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.1)
                        put("responseMimeType", "application/json")
                    })
                }

                val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                    val parsed = parseProfileJson(text)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                } else {
                    Log.w(TAG, "Gemini API error: ${response.code} ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in natural language extraction via Gemini: ${e.message}")
            }
        }

        // High quality intelligent heuristic fallback
        return@withContext fallbackNaturalLanguageExtraction(userText)
    }

    private fun parseProfileJson(rawJson: String): UserProfile? {
        return try {
            val clean = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(clean)

            UserProfile(
                name = obj.optString("name", "Citizen").ifBlank { "Citizen" },
                age = if (obj.has("age") && !obj.isNull("age")) obj.getInt("age") else null,
                gender = obj.optString("gender", "Male").ifBlank { "Male" },
                state = obj.optString("state", "Tamil Nadu").ifBlank { "Tamil Nadu" },
                district = obj.optString("district", "Chennai").ifBlank { "Chennai" },
                occupation = obj.optString("occupation", "Student").ifBlank { "Student" },
                education = obj.optString("education", "Undergraduate").ifBlank { "Undergraduate" },
                annualIncome = if (obj.has("annualIncome") && !obj.isNull("annualIncome")) obj.getLong("annualIncome") else 200000L,
                familySize = if (obj.has("familySize") && !obj.isNull("familySize")) obj.getInt("familySize") else 4,
                isStudent = if (obj.has("isStudent") && !obj.isNull("isStudent")) obj.getBoolean("isStudent") else true,
                isEmployed = if (obj.has("isEmployed") && !obj.isNull("isEmployed")) obj.getBoolean("isEmployed") else false,
                isFarmer = if (obj.has("isFarmer") && !obj.isNull("isFarmer")) obj.getBoolean("isFarmer") else false,
                isBusinessOwner = if (obj.has("isBusinessOwner") && !obj.isNull("isBusinessOwner")) obj.getBoolean("isBusinessOwner") else false,
                socialCategory = if (obj.has("socialCategory") && !obj.isNull("socialCategory")) obj.getString("socialCategory") else "General / OBC",
                disabilityStatus = if (obj.has("disabilityStatus") && !obj.isNull("disabilityStatus")) obj.getString("disabilityStatus") else "No"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing JSON: ${e.message}", e)
            null
        }
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

    suspend fun generateSchemeExplanation(
        matchResult: SchemeMatchResult,
        profile: UserProfile,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String = withContext(Dispatchers.IO) {
        val scheme = matchResult.scheme
        val criteriaPass = matchResult.criteriaResults.filter { it.status == CriterionStatus.PASSED }
        val criteriaFail = matchResult.criteriaResults.filter { it.status == CriterionStatus.FAILED }
        val criteriaMissing = matchResult.criteriaResults.filter { it.status == CriterionStatus.MISSING_INFO }

        if (isKeyConfigured()) {
            try {
                val prompt = """
                    You are Urimai AI, a trustworthy, responsible civic technology explanation engine.
                    Explain the citizen's eligibility status for "${scheme.name}" in simple, accessible, empathetic language.
                    
                    STRICT RULES:
                    1. Rely ONLY on the verified criteria and citizen profile provided below.
                    2. NEVER invent eligibility conditions, document names, deadlines, legal acts, or government promises.
                    3. Do NOT say "You are officially approved" or "Government guarantees this". Say "Likely eligible based on available criteria".
                    4. Target Language: ${language.label} (${language.nativeLabel}).
                    
                    Citizen Profile:
                    - Age: ${profile.age ?: "Not specified"}
                    - Gender: ${profile.gender}
                    - State: ${profile.state}
                    - Occupation: ${profile.occupation}
                    - Education: ${profile.education}
                    - Annual Family Income: ${profile.annualIncome?.let { EligibilityEngine.formatInr(it) } ?: "Not specified"}
                    - Student: ${profile.isStudent} | Farmer: ${profile.isFarmer} | Business: ${profile.isBusinessOwner}
                    
                    Evaluation Result: ${matchResult.status.label}
                    - Passed Conditions (${criteriaPass.size}): ${criteriaPass.joinToString { "${it.criterion.title} (${it.userValueDisplay})" }}
                    - Failed Conditions (${criteriaFail.size}): ${criteriaFail.joinToString { "${it.criterion.title}: ${it.failureReason}" }}
                    - Missing Info (${criteriaMissing.size}): ${criteriaMissing.joinToString { it.criterion.title }}
                    - Required Documents: ${scheme.requiredDocuments.joinToString { it.name }}
                    - Missing Documents for user: ${matchResult.missingDocuments.joinToString { it.name }}
                    
                    Provide a concise 2-3 paragraph plain-language summary with transparent bullet points.
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                    })
                }

                val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val root = JSONObject(response.body?.string() ?: "")
                    val text = root.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext text
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini explanation failed: ${e.message}")
            }
        }

        // Deterministic High Quality Explanation Fallback
        return@withContext buildDeterministicExplanation(matchResult, profile, language)
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

    suspend fun answerCitizenQuestion(
        question: String,
        matchResult: SchemeMatchResult,
        profile: UserProfile,
        language: AppLanguage = AppLanguage.ENGLISH
    ): String = withContext(Dispatchers.IO) {
        val scheme = matchResult.scheme

        if (isKeyConfigured()) {
            try {
                val prompt = """
                    You are Urimai AI Assistant. Answer this citizen's specific question about "${scheme.name}".
                    Question: "$question"
                    
                    STRICT CIVIC TECH RULES:
                    1. Use ONLY the verified data below. Never hallucinate facts, phone numbers, unauthorized promises, or extra rules.
                    2. If the user asks something not in the scheme data, reply: "I don't have enough verified government information to determine that."
                    3. Target Language: ${language.label} (${language.nativeLabel}).
                    
                    Scheme Data:
                    - Department: ${scheme.department}
                    - Benefits: ${scheme.detailedBenefits.joinToString("; ")}
                    - Criteria: ${scheme.criteria.joinToString("; ") { "${it.title}: ${it.requirementDisplay}" }}
                    - Required Documents: ${scheme.requiredDocuments.joinToString("; ") { "${it.name} (${it.stage})" }}
                    - Source: ${scheme.officialSourceLabel} (${scheme.sourceUrl})
                    
                    Citizen Evaluation:
                    - Status: ${matchResult.status.label}
                    - Passed: ${matchResult.criteriaResults.filter { it.status == CriterionStatus.PASSED }.joinToString { it.criterion.title }}
                    - Failed: ${matchResult.criteriaResults.filter { it.status == CriterionStatus.FAILED }.joinToString { "${it.criterion.title} (${it.failureReason})" }}
                    - Missing info: ${matchResult.criteriaResults.filter { it.status == CriterionStatus.MISSING_INFO }.joinToString { it.criterion.title }}
                    - Missing documents: ${matchResult.missingDocuments.joinToString { it.name }}
                """.trimIndent()

                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                    })
                }

                val url = "$BASE_URL/$GEMINI_MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
                val request = Request.Builder()
                    .url(url)
                    .post(requestJson.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val root = JSONObject(response.body?.string() ?: "")
                    val answer = root.optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                    if (!answer.isNullOrBlank()) {
                        return@withContext answer
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Q&A call error: ${e.message}")
            }
        }

        // Fast high quality deterministic answering fallback
        return@withContext fallbackAnswerQuestion(question, matchResult, profile, language)
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
