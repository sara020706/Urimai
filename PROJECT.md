# Urimai — Project Documentation

> **Urimai** (உரிமை, Tamil for *"right"* / *"entitlement"*) is an AI-assisted Government Scheme & Form Eligibility Assistant for Indian citizens.
>
> A citizen fills in one profile. Urimai evaluates that profile against a catalog of 33 real Indian central and Tamil Nadu state welfare schemes using a **transparent, deterministic rule engine**, and explains every verdict — criterion by criterion — in English, Tamil, or Hindi.

**Status:** Prototype. Not an official government service. See [Limitations](#13-known-limitations--rough-edges).

---

## Table of Contents

1. [What the Project Does](#1-what-the-project-does)
2. [System Architecture](#2-system-architecture)
3. [Repository Layout](#3-repository-layout)
4. [The Domain Model](#4-the-domain-model)
5. [The Eligibility Engine](#5-the-eligibility-engine-the-core)
6. [The Scheme Catalog](#6-the-scheme-catalog)
7. [The AI Layer](#7-the-ai-layer)
8. [Android App Architecture](#8-android-app-architecture)
9. [The Backend API](#9-the-backend-api)
10. [Data Flows End to End](#10-data-flows-end-to-end)
11. [Design System](#11-design-system)
12. [Setup & Running](#12-setup--running)
13. [Known Limitations & Rough Edges](#13-known-limitations--rough-edges)
14. [Glossary](#14-glossary)

---

## 1. What the Project Does

### The problem

India runs hundreds of welfare schemes across central ministries and state departments. A citizen who might qualify for six of them typically knows about one. The barriers are concrete:

- **Discovery** — schemes live on dozens of unconnected portals.
- **Eligibility opacity** — criteria are written in bureaucratic language across PDFs.
- **Document ambiguity** — you learn which certificate you're missing *after* being rejected.
- **Language** — most portals are English-first.

### The approach

Urimai inverts the flow. Instead of the citizen searching scheme-by-scheme, they describe themselves **once** and the app evaluates **every** scheme against that description.

The design commitment that shapes the whole codebase:

> **Eligibility verdicts are computed by deterministic rules, never by the AI model.**

The [`EligibilityEngine`](#5-the-eligibility-engine-the-core) is pure Kotlin with no network calls and no model inference. AI is used *only* to translate an already-computed verdict into friendly prose and to answer follow-up questions. This means every verdict is reproducible, auditable, and explainable down to the individual criterion — which the UI surfaces through an **audit trail** component.

### What a user actually sees

1. **Sign up / log in** — username + password.
2. **Analyzing screen** — a 5-step animation while rules evaluate.
3. **Dashboard** — summary counts (likely eligible / more info needed / not matching), plus a filterable, searchable list of all 33 schemes with a status badge on each.
4. **Scheme detail** — benefits, a per-criterion pass/fail breakdown with the user's actual value shown against each requirement, a document checklist, an AI explanation, an audit trail, and a scheme-scoped chat assistant.
5. **Profile edit** — change any field and every scheme re-evaluates reactively.
6. **Saved schemes** — a bookmarked shortlist.

### Three status outcomes

Urimai deliberately avoids a binary eligible/ineligible split:

| Status | Meaning | Trigger |
|---|---|---|
| **Likely eligible** | All criteria passed | No failures, no gaps |
| **More information needed** | Cannot determine yet | ≥1 criterion has missing data, zero failures |
| **Doesn't currently match** | A hard requirement is unmet | ≥1 criterion failed |

The middle state is the point. It converts "no" into "here's the specific question we still need answered," and the wording throughout ("Likely eligible", "Doesn't *currently* match") avoids implying an official determination.

---

## 2. System Architecture

Three tiers, with the rule engine living entirely on-device:

```
┌──────────────────────────────────────────────────────────┐
│  ANDROID APP  (Kotlin · Jetpack Compose · MVVM)          │
│                                                           │
│   UI (Compose screens + components)                       │
│        ↕ StateFlow                                        │
│   UrimaiViewModel  ──────────────────────────────┐        │
│        ↕                                          │        │
│   Repositories (Auth·Profile·Document·Saved)      │        │
│        ↕                                          ↓        │
│   Retrofit + OkHttp                    ┌──────────────────┐│
│                                        │ EligibilityEngine ││
│   SchemeRepository (33 schemes,        │  pure Kotlin      ││
│   hardcoded, on-device)  ─────────────>│  no network       ││
│                                        │  deterministic    ││
│   SessionManager (DataStore: JWT)      └──────────────────┘│
└───────────────┬──────────────────────────────┬───────────┘
                │ HTTPS/HTTP + Bearer JWT      │ HTTPS + API key
                ↓                               ↓
┌───────────────────────────────┐   ┌──────────────────────┐
│  BACKEND  (Node · Express)    │   │  Google Gemini API   │
│  auth · profile · documents   │   │  explanation + chat  │
│  saved-schemes                │   │  (optional)          │
└───────────────┬───────────────┘   └──────────────────────┘
                ↓
┌───────────────────────────────┐
│  POSTGRES (Neon)              │
│  users · user_profiles        │
│  uploaded_documents · saved   │
└───────────────────────────────┘
```

### Why the backend exists

Stated plainly in `backend/README.md`: **an Android app must never embed raw database credentials.** The APK is decompilable, so a bundled `DATABASE_URL` would be a public credential. The Express service holds that secret and exposes authenticated REST endpoints instead.

### Two independent degradation paths

The app is built so that neither external dependency is load-bearing for the core feature:

- **No Gemini key?** `isKeyConfigured()` returns false and every AI call routes to a hand-written deterministic fallback. Explanations and chat still work, just more templated.
- **No backend?** Rule evaluation still runs — the engine and the 33-scheme catalog are compiled into the APK. Only auth, persistence, and document upload require the server.

---

## 3. Repository Layout

```
Urimai/
├── PROJECT.md                     ← this file
├── metadata.json                  App identity + AI Studio capability flags
├── build.gradle.kts               Root Gradle (plugin aliases only)
├── settings.gradle.kts            Repos + module list (:app)
├── gradle/libs.versions.toml      Version catalog — all dependency versions
├── gradle.properties              JVM args, parallel build, config cache
├── .env.example                   GEMINI_API_KEY placeholder
│
├── app/                           ANDROID MODULE
│   ├── build.gradle.kts           SDK levels, signing, secrets plugin, deps
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt            Single activity; Compose entry
│       │   │   ├── ai/
│       │   │   │   └── UrimaiAiService.kt     Gemini REST + fallbacks (32 KB)
│       │   │   ├── data/
│       │   │   │   ├── model/                 Domain types (5 files)
│       │   │   │   ├── remote/                Retrofit, DTOs, session
│       │   │   │   └── repository/
│       │   │   │       ├── SchemeRepository.kt  33-scheme catalog (106 KB)
│       │   │   │       └── …Auth/Profile/Document/SavedSchemes
│       │   │   ├── engine/
│       │   │   │   └── EligibilityEngine.kt   THE CORE — rule evaluation
│       │   │   ├── ui/
│       │   │   │   ├── components/            7 reusable components
│       │   │   │   ├── navigation/            NavHost + destinations
│       │   │   │   ├── screens/               6 screens
│       │   │   │   └── theme/                 Civic Trust palette
│       │   │   └── viewmodel/
│       │   │       └── UrimaiViewModel.kt     All app state
│       │   └── res/                           Icons, colors, strings
│       ├── test/                              Unit + Robolectric + screenshot
│       └── androidTest/                       Instrumented
│
└── backend/                       NODE MODULE
    ├── README.md                  Backend-specific setup
    ├── package.json
    └── src/
        ├── server.js              Express bootstrap, route mounting
        ├── db/pool.js             pg Pool (SSL)
        ├── db/migrate.js          Idempotent CREATE TABLE IF NOT EXISTS
        ├── middleware/auth.js     JWT bearer verification
        └── routes/                auth · profile · documents · savedSchemes
```

**Two files dominate by size and deserve special mention:**

- `SchemeRepository.kt` (106 KB) — not logic, but *data*. A hand-curated declarative catalog of 33 schemes. Large because each scheme carries trilingual names, benefits, criteria with explanatory text, and documents with tips.
- `UrimaiAiService.kt` (32 KB) — large because every AI function ships with a complete non-AI fallback implementation alongside it.

---

## 4. The Domain Model

All types live in `app/src/main/java/com/example/data/model/`.

### `UserProfile` — the single input

One immutable data class is the sole input to eligibility. Every field is pre-populated with a sensible default so the app is usable immediately; nullable fields represent genuinely unknown data.

| Field | Type | Default | Role |
|---|---|---|---|
| `name` | `String` | `"Citizen"` | Display only |
| `age` | `Int?` | `21` | `MIN_AGE` / `MAX_AGE` |
| `gender` | `String` | `"Male"` | `GENDER_MATCH` |
| `state` / `district` | `String` | `"Tamil Nadu"` / `"Chennai"` | `STATE_MATCH` |
| `occupation` | `String` | `"Student"` | Display / context |
| `education` | `String` | `"Undergraduate"` | `EDUCATION_LEVEL_IN` |
| `annualIncome` | `Long?` | `200000` | `MAX_INCOME` / `MIN_INCOME` |
| `familySize` | `Int` | `4` | `FAMILY_SIZE_MIN` |
| `isStudent` / `isEmployed` / `isFarmer` / `isBusinessOwner` | `Boolean?` | `true`/`false`/`false`/`false` | Status criteria |
| `socialCategory` | `String?` | `"General / OBC"` | `SOCIAL_CATEGORY_IN` |
| `disabilityStatus` | `String?` | `"No"` | `DISABILITY_STATUS` |
| `maritalStatus` | `String?` | `"Single"` | Context |
| `ownedDocuments` | `Set<String>` | 4 common docs | Document checklist |

Two computed helpers: `isComplete` (are the load-bearing fields present?) and `getMissingRequiredFields()` (which ones aren't, by display name).

The **nullable `Boolean?`** pattern is deliberate and carries three meanings — `true`, `false`, and `null` = "we haven't asked." Only `null` produces the *More information needed* status; it's what keeps "unknown" from being silently treated as "no."

### `Scheme` and its parts

```kotlin
data class Scheme(
    val id: String, val name: String, val shortName: String,
    val tamilName: String, val hindiName: String,     // trilingual
    val category: SchemeCategory,
    val department: String,                            // issuing authority
    val description: String,
    val benefitHighlight: String,                      // one-line hook
    val detailedBenefits: List<String>,
    val criteria: List<EligibilityCriterion>,          // the rules
    val requiredDocuments: List<SchemeDocument>,
    val officialSourceLabel: String,
    val sourceUrl: String,                             // real .gov.in link
    val lastVerifiedDate: String,
    val applicationMethod: String,
    val applicationSteps: List<String>
)
```

`EligibilityCriterion` is where the transparency commitment becomes structural. Beyond the machine-readable `conditionType` + `targetValue`, **three separate human-facing text fields** are mandatory:

```kotlin
data class EligibilityCriterion(
    val id: String,
    val title: String,
    val conditionType: CriterionConditionType,  // how to evaluate
    val targetValue: Any,                       // what to compare against
    val requirementDisplay: String,             // "Age must be 18–35 years"
    val explanationNote: String,                // why this rule exists
    val whyWeAskReason: String                  // why we need YOUR data
)
```

That last field — `whyWeAskReason` — is a privacy-facing justification surfaced in the UI. The type system makes it impossible to add a criterion without explaining to the citizen why their data is being collected for it.

`SchemeDocument` carries `isMandatoryForEligibility`, a `stage` (Application Submission vs. Document Verification), and a practical `tip`.

### Result types

`CriterionEvaluation` pairs a criterion with its `CriterionStatus` (`PASSED` / `FAILED` / `MISSING_INFO`), the user's actual value as a display string, and a `failureReason` when applicable.

`SchemeMatchResult` aggregates everything for one scheme — overall status, all evaluations, pre-partitioned `missingCriteria` / `failedCriteria` lists, document readiness split into `readyDocuments` / `missingDocuments`, and a natural-language `ruleSummary`.

`DashboardSummary` holds the five headline counts.

---

## 5. The Eligibility Engine (The Core)

**File:** `app/src/main/java/com/example/engine/EligibilityEngine.kt`

A stateless Kotlin `object`. No network. No AI. No I/O. Same profile + same scheme ⇒ same verdict, always.

### Evaluation algorithm

```kotlin
fun evaluateScheme(profile: UserProfile, scheme: Scheme): SchemeMatchResult {
    // 1. Evaluate every criterion independently
    val criteriaResults = scheme.criteria.map { evaluateCriterion(profile, it) }

    // 2. Aggregate — failure dominates, then missing info
    val status = when {
        failedCriteria.isNotEmpty()  -> EligibilityStatus.NOT_ELIGIBLE
        missingCriteria.isNotEmpty() -> EligibilityStatus.MORE_INFO_NEEDED
        else                         -> EligibilityStatus.LIKELY_ELIGIBLE
    }

    // 3. Match documents, 4. Build summary, 5. Assemble result
}
```

Two properties worth naming:

- **No early exit.** Every criterion is evaluated even after one fails, because the UI shows the complete breakdown. A citizen seeing "7 of 8 passed, age is the only gap" learns something actionable that an early return would have discarded.
- **Failure dominates missing info.** If one rule definitively fails, the answer is *Doesn't match* — no point asking for more data.

### The 14 condition types

Each is a branch in `evaluateCriterion`, and each handles the missing-data case explicitly.

| Condition Type | Compares | Notes |
|---|---|---|
| `MIN_AGE` / `MAX_AGE` | `profile.age` | Default targets 18 / 35 |
| `MAX_INCOME` / `MIN_INCOME` | `profile.annualIncome` | Failure message quantifies the excess |
| `STATE_MATCH` | `profile.state` | Target `"All"` = nationwide |
| `GENDER_MATCH` | `profile.gender` | `"Prefer not to say"` ⇒ `MISSING_INFO` |
| `EDUCATION_LEVEL_IN` | `profile.education` | Set membership, case-insensitive |
| `STUDENT_STATUS` | `isStudent` | `null` ⇒ `MISSING_INFO` |
| `EMPLOYED_STATUS` | `isEmployed` | Some schemes require *un*employment |
| `FARMER_STATUS` | `isFarmer` | |
| `BUSINESS_OWNER_STATUS` | `isBusinessOwner` | |
| `SOCIAL_CATEGORY_IN` | `socialCategory` **or** `gender` | See below |
| `DISABILITY_STATUS` | `disabilityStatus` | Any value ≠ `"No"` passes |
| `FAMILY_SIZE_MIN` | `familySize` | Non-null, cannot be missing |

**`SOCIAL_CATEGORY_IN` is the one disjunctive rule.** It passes if the user is in an allowed category *or* is female — modeling Stand-Up India, which mandates lending to SC/ST **or** women entrepreneurs:

```kotlin
val passesViaGender   = userGender.equals("Female", ignoreCase = true)
val passesViaCategory = userCategory != null &&
                        allowedList.any { userCategory.contains(it, ignoreCase = true) }
if (passesViaGender || passesViaCategory) { /* PASSED */ }
```

### Failure messages are quantified

Rather than "income too high," the engine computes the gap:

```
"Income ₹5,00,000 exceeds the ceiling limit of ₹3,00,000 by ₹2,00,000."
```

Currency runs through `formatInr()` using `NumberFormat.getCurrencyInstance(Locale("en","IN"))`, producing the Indian lakh/crore digit grouping (₹2,00,000 rather than ₹200,000), with a `try/catch` falling back to `"₹$amount"`.

### Document matching

Bidirectional substring matching, case-insensitive, to tolerate naming variation between the user's list and each scheme's:

```kotlin
owned.contains(doc.name, ignoreCase = true) || doc.name.contains(owned, ignoreCase = true)
```

This is forgiving by design — "Aadhaar" matches "Aadhaar Card". It's also loose enough to produce occasional false matches on short names; see [Limitations](#13-known-limitations--rough-edges).

### Dashboard aggregation

`computeDashboardSummary()` counts each status, and computes `missingDocumentsCount` as the **distinct-by-name** documents missing across *only the likely-eligible* schemes — answering the genuinely useful question: "what should I go obtain to act on what I already qualify for?"

---

## 6. The Scheme Catalog

**File:** `app/src/main/java/com/example/data/repository/SchemeRepository.kt` — a single `object` exposing `allSchemes: List<Scheme>`, hardcoded and compiled into the APK.

**33 schemes**, each with a real `sourceUrl` pointing at the official portal.

### By category

| Category | Count | Examples |
|---|---|---|
| Social Welfare | 10 | Ayushman Bharat PM-JAY, PM Ujjwala, Janani Suraksha, IGNOAPS/IGNDPS/IGNWPS pensions, TN Free Bus Travel |
| Education | 6 | PM Vidya Lakshmi, Pudhumai Penn, NMMSS, TN Free Laptop, TN CM Breakfast, AICTE Saksham |
| Entrepreneurship | 5 | PMEGP, Stand-Up India, PM MUDRA, Startup India Seed Fund, PM SVANidhi |
| Employment | 3 | MGNREGA, TN CM Fellowship, NCS Employment Portal |
| Agriculture | 3 | PM-KISAN, PM Fasal Bima, Kisan Credit Card |
| Skill Development | 2 | PMKVY, DAY-NULM |
| Housing | 2 | PMAY, PM Surya Ghar Solar |
| Financial Assistance | 2 | Sukanya Samriddhi, Atal Pension |

The mix is intentional: central schemes give national relevance, and a Tamil Nadu cluster (Pudhumai Penn, CM Fellowship, Free Laptop, Free Bus Travel, CM Breakfast, Marriage Assistance) demonstrates state-level coverage matching the app's Tamil-first identity.

### Why hardcoded?

Trade-off, consciously made:

- **Gains** — zero-latency evaluation, full offline function, no scraping infrastructure, no API to go stale mid-session, reviewable data under version control.
- **Costs** — scheme changes require an app release; the catalog can drift from reality (hence the `lastVerifiedDate` field on every scheme).

For a prototype demonstrating the *evaluation model*, that's the right trade. A production version would move this behind a versioned content API — and the `Scheme` data class is already shaped to serialize cleanly, so the change is localized to this one file.

---

## 7. The AI Layer

**File:** `app/src/main/java/com/example/ai/UrimaiAiService.kt`

```kotlin
private const val GEMINI_MODEL = "gemini-3.5-flash"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
```

Direct REST via OkHttp with hand-built `org.json` payloads (30s timeouts) rather than an SDK — keeping the dependency surface small and the request shape explicit.

### The boundary — what AI may and may not do

| AI **never** does | AI **does** |
|---|---|
| Decide eligibility | Explain a verdict in plain language |
| Compute criterion pass/fail | Translate explanations to Tamil / Hindi |
| Alter the profile behind the user's back | Answer scheme-scoped follow-up questions |

Every AI call receives an **already-computed** `SchemeMatchResult` and is asked only to narrate it. A model outage, hallucination, or rate limit can degrade the *wording* of an explanation; it cannot change who is eligible.

### Key gating

```kotlin
private fun isKeyConfigured(): Boolean = try {
    val key = BuildConfig.GEMINI_API_KEY
    key.isNotBlank() && !key.equals("MY_GEMINI_API_KEY", ignoreCase = true)
} catch (_: Throwable) { false }
```

The placeholder check matters — the Secrets Gradle plugin injects the `.env.example` placeholder when no real `.env` exists, so a plain blank-check would let placeholder calls through and fail at the network layer. The `catch (_: Throwable)` guards `BuildConfig` field absence.

### The three functions

**1. `generateSchemeExplanation(matchResult, profile, language)`** — the main path. Sends the scheme, the partitioned pass/fail/missing criteria, and the target language; returns a citizen-readable explanation. Falls back to `buildDeterministicExplanation()`, which composes the same content from the same data using string templates.

**2. `answerCitizenQuestion(question, matchResult, profile, language)`** — powers the per-scheme chat. Scoped to the selected scheme so answers stay grounded in that scheme's actual criteria. Falls back to `fallbackAnswerQuestion()`, a keyword-routed responder.

**3. `extractProfileFromNaturalLanguage(userText)`** — parses free text ("I'm a 22-year-old student from Chennai, family income 2 lakh") into a `UserProfile`, requesting `responseMimeType: "application/json"` at `temperature 0.1`. Its fallback, `fallbackNaturalLanguageExtraction()`, is a genuinely capable regex parser that handles Indian numeric conventions — `"2.5 lakh"` → `250000`, plus `₹`/`Rs.`/`INR` prefixes and bare 5–7 digit numbers.

> ⚠️ **This third function is currently dead code** — no caller anywhere in the app. It appears to be built-ahead infrastructure for a conversational onboarding screen that isn't wired up. See [Limitations](#13-known-limitations--rough-edges).

### Response parsing

Defensive throughout — `optJSONArray`/`optJSONObject`/`optString` at each level so a shape change yields the fallback rather than a crash, and markdown fence stripping before JSON parse:

```kotlin
val clean = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
```

---

## 8. Android App Architecture

**Stack:** Kotlin · Jetpack Compose (Material 3) · MVVM · Navigation Compose · StateFlow · Retrofit/OkHttp/Moshi · DataStore
**SDK:** min 24 (Android 7.0) · target/compile 36 · `applicationId` `com.aistudio.urimai.cvkgrt`
**Permission:** `INTERNET` only — no camera, no location, no storage, no contacts.

### `MainActivity`

Single-activity. Enables edge-to-edge, applies `MyApplicationTheme`, hoists one `UrimaiViewModel`, and hands it to `UrimaiApp`. ~30 lines.

### `UrimaiViewModel` — the state hub

An `AndroidViewModel` (needs `Context` for repositories) owning four repositories and exposing everything as `StateFlow`.

**The architectural centerpiece is that results are *derived*, not stored:**

```kotlin
val allEvaluationResults: StateFlow<List<SchemeMatchResult>> = _userProfile.map { profile ->
    EligibilityEngine.evaluateAll(profile, SchemeRepository.allSchemes)
}.stateIn(viewModelScope, SharingStarted.Eagerly,
          EligibilityEngine.evaluateAll(UserProfile(), SchemeRepository.allSchemes))
```

Results are a pure function of the profile. Changing one field in the editor re-evaluates all 33 schemes and every dependent piece of UI updates automatically — there is no cache to invalidate and no way for displayed results to drift from the current profile. Cheap enough to run eagerly since the engine is in-memory arithmetic and string comparison.

Three further flows derive from that one:

```kotlin
filteredMatches      = combine(allEvaluationResults, category, statusFilter, searchQuery) { … }
dashboardSummary     = allEvaluationResults.map(EligibilityEngine::computeDashboardSummary)
selectedSchemeResult = combine(allEvaluationResults, selectedSchemeId) { … }
```

Search spans English, Tamil, and Hindi names plus department — so a Tamil speaker can find a scheme by its Tamil name.

**Optimistic updates** on bookmarking — local state flips immediately, the network call follows:

```kotlin
fun toggleSaveScheme(schemeId: String) {
    _savedSchemeIds.value = current       // UI updates now
    viewModelScope.launch {               // server catches up
        if (nowSaved) savedSchemesRepository.saveScheme(schemeId)
        else savedSchemesRepository.unsaveScheme(schemeId)
    }
}
```

**Session restoration** runs in `init` — a stored token navigates straight to the dashboard, skipping login.

### Navigation

Six destinations in `UrimaiDestinations`: `LOGIN`, `ANALYZING`, `DASHBOARD`, `SCHEME_DETAIL`, `SAVED_SCHEMES`, `PROFILE_VIEW_EDIT`.

Login transition uses `popUpTo(LOGIN) { inclusive = true }` so back doesn't return to a stale login screen; logout uses `popUpTo(0)` to clear the entire stack.

The **analyzing animation** (5 steps × 450ms + 300ms ≈ 2.5s) is honest theater: the engine finishes in milliseconds. It serves two real purposes — signaling that per-criterion work is happening, and covering profile-save latency after an edit. Worth knowing it's cosmetic.

### Screens and components

| Screen | Role |
|---|---|
| `AuthScreen` | Login/signup, mode toggle, inline errors |
| `AnalyzingScreen` | Stepped progress animation |
| `MatchesDashboardScreen` | Summary cards, category/status filters, search, scheme list (21 KB) |
| `SchemeDetailScreen` | Benefits, criteria breakdown, documents, AI explanation, audit trail, chat (33 KB) |
| `ProfileViewEditScreen` | Full profile editor + document upload + logout |
| `SavedSchemesScreen` | Bookmarked shortlist |

| Component | Role |
|---|---|
| `SchemeCard` | List item with status badge and match count |
| `CriterionRow` | One criterion: requirement, user's value, verdict, why-we-ask |
| `DocumentChecklist` | Ready vs. missing documents |
| `DocumentUpload` | File picker + upload row |
| `AuditTrailSection` | Step-by-step trace of how the verdict was reached |
| `CommonUi` | `StatusBadge`, `TrustPill`, `LanguageSelector`, `CivicHeader`, `PrivacyNoticeCard`, `DisclaimerBanner`, `HowItWorksBottomSheet` |

`PrivacyNoticeCard`, `DisclaimerBanner`, `TrustPill`, and `AuditTrailSection` exist purely to make the system legible and non-authoritative to the user — the trust posture is a first-class feature, not decoration.

---

## 9. The Backend API

**Stack:** Node.js · Express 4 · PostgreSQL (Neon) · bcryptjs · jsonwebtoken · multer · cors · dotenv

### Structure

`server.js` loads env, applies `cors()` + `express.json()`, exposes `GET /health`, mounts four routers, and installs a catch-all error handler that logs the error and returns a generic 500 (no internals leaked to the client).

`db/pool.js` fails fast at import if `DATABASE_URL` is unset — a clear startup error instead of a confusing runtime one. Uses `ssl: { rejectUnauthorized: false }` for Neon.

### Schema

```sql
users              (id, username UNIQUE, password_hash, display_name, created_at)
user_profiles      (user_id PK→users, …17 profile columns…, owned_documents TEXT[], updated_at)
uploaded_documents (id, user_id→users, document_name, file_name, mime_type, file_data BYTEA, uploaded_at)
saved_schemes      (user_id→users, scheme_id, saved_at, PRIMARY KEY (user_id, scheme_id))
```

All child tables cascade on user delete. `migrate.js` is idempotent (`CREATE TABLE IF NOT EXISTS`) and safe to re-run.

Two schema notes: `user_profiles` mirrors `UserProfile` field-for-field with DB defaults matching the Kotlin defaults, so a fresh signup lands on a working profile. `saved_schemes` uses a composite PK, making saves naturally idempotent via `ON CONFLICT DO NOTHING`.

### Endpoints

| Method | Path | Auth | Purpose |
|---|---|---|---|
| `GET` | `/health` | — | Liveness |
| `POST` | `/auth/signup` | — | Create user + blank profile → `{userId, displayName, token}` |
| `POST` | `/auth/login` | — | Verify credentials → same shape |
| `GET` | `/profile` | ✔ | Fetch profile |
| `PUT` | `/profile` | ✔ | Full replace, returns updated |
| `GET` | `/documents` | ✔ | List metadata (no bytes) |
| `POST` | `/documents` | ✔ | Multipart upload → `bytea` |
| `GET` | `/documents/:id/file` | ✔ | Stream raw file |
| `DELETE` | `/documents/:id` | ✔ | Delete |
| `GET` | `/saved-schemes` | ✔ | Array of scheme IDs |
| `PUT` | `/saved-schemes/:schemeId` | ✔ | Save (idempotent) |
| `DELETE` | `/saved-schemes/:schemeId` | ✔ | Unsave |

Auth via `Authorization: Bearer <token>`.

### Security posture

**Done correctly:**
- bcrypt hashing, cost factor 10 — plaintext never stored.
- Parameterized queries everywhere (`$1, $2, …`) — no SQL injection surface.
- **Ownership enforced in the WHERE clause,** not just by authentication:
  ```sql
  SELECT … FROM uploaded_documents WHERE id = $1 AND user_id = $2
  ```
  A valid token for user A cannot read user B's document by guessing an ID.
- Usernames normalized (trim + lowercase) consistently on both signup and login.
- Upload capped at 10 MB via multer.
- 30-day JWT expiry.
- `backend/.gitignore` excludes `.env` — and `git ls-files` confirms the real `.env` is untracked.

**Explicitly prototype-grade** (the backend README says so):
- No rate limiting — brute-force login is open.
- No refresh-token rotation; a leaked 30-day token stays valid.
- No email verification or password reset.
- 4-character minimum password.
- No HTTPS termination — must sit behind a platform that provides it.
- `cors()` with no origin restriction.
- Files stored as `bytea` in Postgres — simple, but doesn't scale; object storage with signed URLs is the production answer.
- `Content-Disposition: inline` with a user-supplied filename on document download.

---

## 10. Data Flows End to End

### Login → dashboard

```
AuthScreen → viewModel.logIn()
  → AuthRepository.logIn()  [normalize username, validate]
    → POST /auth/login → bcrypt.compare → JWT
      → SessionManager.saveSession(token, userId, displayName)   [DataStore]
      → authState.isLoggedIn = true
        → LaunchedEffect navigates to ANALYZING
        → loadRemoteData(): profile + documents + saved schemes in parallel
          → _userProfile update triggers evaluateAll(33 schemes)
            → dashboardSummary / filteredMatches recompute
              → 2.5s animation completes → DASHBOARD
```

Note `loadRemoteData()` launches three independent coroutines rather than awaiting sequentially.

### Profile edit → re-evaluation

```
ProfileViewEditScreen → viewModel.updateProfile(newProfile)
  ├─ _userProfile.value = newProfile        (immediate, local)
  │    └─ allEvaluationResults recomputes   (automatic, reactive)
  │         └─ every dependent flow + UI updates
  └─ persistProfile() → PUT /profile        (async, fire-and-forget)
```

The UI never waits on the network to show updated results — and because results derive from the profile flow, there's no path where they're stale.

### Scheme detail → AI explanation

```
Tap scheme → viewModel.selectScheme(id)
  ├─ _selectedSchemeId = id → selectedSchemeResult resolves
  ├─ loadSchemeExplanation(id)
  │    ├─ cached in _aiExplanationMap? → return (no call)
  │    └─ else UrimaiAiService.generateSchemeExplanation(result, profile, language)
  │         ├─ key configured? → Gemini REST
  │         └─ else / on failure → buildDeterministicExplanation()
  └─ initChatForScheme(id) → seed greeting message
```

Explanations are cached per scheme; a language change calls with `forceReload = true` to regenerate in the new language.

### Document upload

```
Pick file (SAF URI) → viewModel.uploadDocument(name, uri, fileName, mimeType)
  → DocumentRepository: copy URI → temp file in cacheDir
    → multipart POST /documents → stored as bytea
      → finally { tempFile.delete() }        ← runs even on failure
        → add to _uploadedDocuments
        → toggleDocumentOwnedIfMissing(name) → profile.ownedDocuments += name
          → persistProfile()
            → re-evaluation → document checklists update across all schemes
```

Uploading a document immediately improves document-readiness on every scheme that requires it — the `finally` block guarantees temp cleanup regardless of outcome.

---

## 11. Design System

### Civic Trust palette

Defined in `ui/theme/Color.kt`, chosen to read as official-but-approachable rather than corporate:

- **Civic Navy** (`#0C233C` → `#F1F5F9`) — institutional trust, primary surfaces
- **Saffron** (`#B54708` / `#E06915` / `#FF8E3C`) — national accent, CTAs
- **Emerald** (`#027A48` / `#039855`) — passed criteria, likely eligible
- **Amber** (`#B54708` / `#F79009`) — missing info, needs review
- **Crimson** (`#B42318` / `#D92D20`) — failed criteria
- **Neutrals** — full light and dark surface/border/text ramps

The semantic triple (Emerald / Amber / Crimson) maps exactly onto `CriterionStatus` and `EligibilityStatus`, so the color a user sees always corresponds to an engine state — never a designer's judgment call. Each has a `Container` variant for backgrounds and a `Text` variant for accessible contrast on it.

### Multilingual support

`AppLanguage` — English, Tamil (`தமிழ்`), Hindi (`हिन्दी`), each with `code`, `label`, and `nativeLabel` so the selector shows each language *in its own script*.

Every scheme carries `tamilName` and `hindiName`; AI explanations and chat respond in the selected language; search matches across all three.

Worth being precise about scope: **scheme content is trilingual; static UI chrome is not.** `strings.xml` holds only `app_name`, and screen labels are hardcoded English. The language selector changes scheme names and AI output — not buttons and headings.

---

## 12. Setup & Running

### Prerequisites

- JDK 11+, Android Studio (AGP 9.1.1, Kotlin 2.2.10)
- Node.js 18+ for the backend
- A Postgres database (Neon recommended)
- *Optional:* a Google Gemini API key

### Backend

```bash
cd backend
npm install
cp .env.example .env     # fill in DATABASE_URL and JWT_SECRET
npm run migrate          # idempotent — safe to re-run
npm start                # http://localhost:4000   (npm run dev to watch)
```

`.env` keys:

```
DATABASE_URL=postgresql://user:password@host/dbname?sslmode=require
JWT_SECRET=<long random string>
PORT=4000
```

Verify with `curl http://localhost:4000/health` → `{"status":"ok"}`.

### Android app

**1. Point the app at your backend.** `data/remote/ApiClient.kt`:

```kotlin
const val BASE_URL = "http://10.0.2.2:4000/"
```

| Target | Value |
|---|---|
| Emulator + local backend | `http://10.0.2.2:4000/` (emulator alias for host localhost) |
| Physical device, same LAN | `http://192.168.x.x:4000/` (your machine's IP) |
| Deployed backend | `https://your-backend.onrender.com/` |

**2. Optional — enable AI.** Create `.env` in the project root:

```
GEMINI_API_KEY=your_actual_key
```

The Secrets Gradle plugin injects it into `BuildConfig`. Skip this and the deterministic fallbacks handle everything.

**3. Build and run.**

```bash
./gradlew assembleDebug        # or Run ▶ in Android Studio
./gradlew installDebug
```

Release builds read `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` from the environment.

### Tests

```bash
./gradlew test                 # unit + Robolectric
./gradlew connectedAndroidTest # instrumented (device required)
```

⚠️ The test suite does not currently compile or pass as-is — see below.

---

## 13. Known Limitations & Rough Edges

Honest inventory, roughly by severity.

### Broken tests

Two of the four tests are template leftovers that were never updated:

1. **`GreetingScreenshotTest` will not compile.** It calls `Greeting("Robolectric")`, and no `Greeting` composable exists anywhere in the source tree (verified by grep). It also captures to `src/test/screenshots/greeting.png`, which is a committed stale artifact. **This breaks `./gradlew test` for the whole module.**
2. **`ExampleRobolectricTest` asserts the wrong value.** It expects `app_name == "My Application"`, but `strings.xml` defines `Urimai` — a guaranteed failure once compilation is fixed.

`ExampleUnitTest` (`assertEquals(4, 2+2)`) is a harmless placeholder.

**Net effect: there is no real test coverage of the eligibility engine** — which is the component that most warrants it, being pure, deterministic, and trivially unit-testable. Deleting the two broken tests and adding `EligibilityEngine` cases would be the single highest-value change to this repo.

### Dead code

`extractProfileFromNaturalLanguage()` plus its ~100-line regex fallback has **no callers**. Either wire up conversational onboarding or remove it; leaving it invites the assumption that the feature exists.

### Security (prototype-grade, acknowledged)

No rate limiting, no refresh tokens, no email verification, 4-char password minimum, unrestricted CORS, cleartext HTTP by default. The backend README is upfront about all of this. None should reach production, and the first three matter most for an app handling income, caste, and disability data.

### Architecture

- **The catalog is compiled in.** Scheme changes require an app release. `lastVerifiedDate` (currently `"August 2026"` across the board) acknowledges drift but doesn't prevent it.
- **Document matching is loose.** Bidirectional substring matching can produce false positives on short document names — a short owned string could match an unintended scheme document.
- **Full-replace profile updates.** `PUT /profile` overwrites every column; a client sending a partial object silently resets omitted fields to defaults. Concurrent edits from two devices will clobber each other.
- **Fire-and-forget persistence.** `persistProfile()` ignores the result — a failed save leaves local and server state divergent with no user-visible signal.
- **`runBlocking` in the auth interceptor.** `ApiClient`'s interceptor calls `runBlocking { sessionManager.currentToken() }` on every request. It's on an OkHttp background thread so it won't ANR, but it blocks that thread on a DataStore read per request; caching the token in memory would be cleaner.
- **Documents as `bytea`.** Fine at prototype scale, poor beyond it.
- **No pagination.** All 33 schemes evaluate and render at once — fine now, not at 500 schemes.
- **`UserProfile` defaults are opinionated.** A new user starts as a 21-year-old Chennai student earning ₹2L. Convenient for demos, but a user who skips the editor gets results for someone else's profile. Defaulting the nullable fields to `null` would surface *More information needed* honestly.

### Product

- **UI chrome isn't translated** — only scheme content and AI output are trilingual.
- **The analyzing animation is cosmetic** — ~2.5s of artificial delay over a millisecond computation.
- **No accessibility audit** — no TalkBack pass or content-description verification evident, notable for an app serving citizens with disabilities and one that lists a disability pension scheme.
- **Not an official service.** Verdicts are informational and depend on catalog accuracy. The in-app `DisclaimerBanner` is doing important work.

---

## 14. Glossary

| Term | Meaning |
|---|---|
| **Urimai** (உரிமை) | Tamil for "right" or "entitlement" |
| **Criterion** | One testable eligibility rule (e.g., age ≤ 35) |
| **Condition type** | One of 14 machine-readable comparison kinds |
| **Likely eligible** | All criteria passed — deliberately not a guarantee |
| **More info needed** | ≥1 criterion has missing data, none failed |
| **Audit trail** | UI trace showing how a verdict was reached |
| **CSC** | Common Service Center — government-run assisted-access point |
| **Lakh** | 100,000 (Indian numbering) |
| **Aadhaar** | India's 12-digit biometric identity number |
| **UDID** | Unique Disability ID card |
| **Patta** | Land ownership record |
| **Bonafide certificate** | School/college proof of enrollment |
| **SC / ST / OBC / EWS** | Scheduled Caste / Scheduled Tribe / Other Backward Class / Economically Weaker Section |
| **PM- / PMAY / PMKVY …** | Pradhan Mantri (Prime Minister's) scheme prefixes |
| **TN** | Tamil Nadu |

---

*Documentation generated from source analysis of the Urimai repository. For backend-specific setup, see [`backend/README.md`](backend/README.md).*
