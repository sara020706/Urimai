# PROJECT: Citizen Government Scheme Eligibility & Legal Assistance Platform

You are an expert **Flutter mobile developer, backend architect, UI/UX designer, database designer, AI engineer, cybersecurity engineer, and product researcher**.

I want to develop a production-oriented mobile application that helps citizens discover government welfare schemes they may be eligible for and connects them directly with verified legal specialists when they need assistance.

The application will be developed using **Flutter/Dart** for the mobile frontend.

Before writing ANY application code, you must deeply research similar existing applications, systems, academic projects, government platforms, and commercial products.

---

# PART 1 — MANDATORY DEEP RESEARCH BEFORE DEVELOPMENT

Do NOT start coding yet.

First perform comprehensive research on existing systems related to this project.

Research at least these categories:

### A. Government Scheme Discovery

Research systems such as:

* myScheme
* UMANG
* JanLabh
* Haqdaar
* SchemesinIndia
* Yojana Setu
* Yojana Mitra
* SchemeGENIE
* Other Indian government-scheme eligibility platforms

Find:

* How they collect user information
* How eligibility matching works
* Whether they use rule-based systems or AI
* Whether they support personalized recommendations
* Whether they support document uploads
* Whether they extract information from certificates
* How schemes are represented in databases
* How they explain eligibility
* How they handle incorrect/missing information
* How they rank schemes
* Their limitations

---

### B. Legal Assistance Platforms

Research:

* LawTribe
* LegalKart
* LawRato
* Avvo
* Tele-Law
* TrueLawyer
* Other relevant Indian and international legal-assistance platforms

Study:

* Lawyer registration
* Lawyer verification
* Lawyer document verification
* Lawyer directories
* Legal Q&A
* Anonymous questions
* Multiple lawyer responses
* Direct lawyer contact
* Chat/communication
* Lawyer privacy
* User privacy
* Moderation
* Abuse prevention
* Payment/consultation models where relevant

---

### C. Combined Civic + Legal + Welfare Platforms

Research systems that combine government benefits, welfare assistance, civic assistance, legal aid, or social support.

Pay particular attention to systems such as:

* Janman India Community
* Praja Arivu / RTI Mitra
* Tele-Law
* SARTHIE-related initiatives
* Similar civic-tech platforms

Determine how they connect citizens with government benefits and/or legal assistance.

---

### D. Document-Based Profile Creation

Research systems that allow:

```text
Government Certificate
        ↓
OCR / Document AI
        ↓
Information Extraction
        ↓
Citizen Profile
        ↓
Eligibility Matching
```

Research:

* OCR approaches
* Document AI
* Structured extraction
* Validation
* User confirmation
* Fraud/tampering considerations
* Privacy
* Data storage
* Supported Indian documents

Pay particular attention to SchemeGENIE and similar systems.

---

### E. Academic Research

Search academic papers, conference papers, journals, GitHub projects, Smart India Hackathon projects, and student projects related to:

* Government scheme recommendation
* Welfare scheme eligibility
* Rule-based eligibility engines
* AI-based scheme recommendation
* Document-aware scheme matching
* Legal assistance platforms
* Legal Q&A systems
* Lawyer recommendation
* Citizen entitlement systems

Do not limit the research to commercial applications.

---

# PART 2 — RESEARCH REQUIREMENTS

For every important existing system, determine:

1. What problem does it solve?
2. Target users
3. Main features
4. Technology if publicly documented
5. Eligibility/matching methodology
6. AI usage
7. Document processing
8. Lawyer verification methodology
9. Privacy model
10. Strengths
11. Limitations
12. How it overlaps with this proposed application

Create a comparison table.

Example:

| System | Scheme Matching | Document Extraction | Legal Q&A | Multiple Lawyers | Lawyer Directory | Verification |
| ------ | --------------- | ------------------- | --------- | ---------------- | ---------------- | ------------ |

Do not falsely claim that this project is completely unique.

Instead identify:

* Existing features
* Existing combinations
* Missing combinations
* Potential differentiation

---

# PART 3 — DETERMINE THE DIFFERENTIATION

After the research, identify whether the proposed application combines existing capabilities in a useful new workflow.

The intended workflow is:

```text
Citizen
   ↓
Create Account
   ↓
Create Profile OR Upload Certificates
   ↓
Profile Extraction / Confirmation
   ↓
Eligibility Engine
   ↓
Government Scheme Matching
   ↓
Ranked Results
   ↓
Explain Why Eligible / Not Eligible
   ↓
User Encounters a Problem
   ↓
Anonymous Legal/Scheme Question
   ↓
Verified Lawyers
   ↓
Independent Lawyer Responses
   ↓
User Sees Multiple Responses
   ↓
Direct Contact With Lawyer
```

Investigate whether an existing product already provides this complete workflow.

If something already exists, clearly identify it.

Do NOT invent novelty claims.

---

# PART 4 — PROPOSED APPLICATION

The application has three roles:

```text
1. Normal User / Citizen
2. Legal Specialist / Lawyer
3. Administrator
```

---

# PART 5 — NORMAL USER

A normal user can create an account and enter the application immediately.

Profile completion is NOT mandatory just to access the application.

The user should be able to browse general content without having a completed profile.

However, personalized eligibility matching requires a profile.

The user has two ways to create one.

## Option A — Manual Profile

The user enters relevant information such as:

* Name
* Age / Date of birth
* Gender
* State
* District
* Occupation
* Employment status
* Annual income
* Education
* Family information
* Marital status where relevant
* Social/economic category where legally relevant
* Disability status where relevant
* Other scheme-specific attributes

Do not collect unnecessary information.

---

# PART 6 — CERTIFICATE-BASED PROFILE CREATION

The user can upload government certificates/documents.

Possible examples:

* Income certificate
* Community/category certificate
* Residence certificate
* Disability certificate
* Educational certificate
* Other relevant government documents

The system should:

```text
Upload Document
       ↓
Document Processing
       ↓
OCR / Document AI
       ↓
Extract Relevant Fields
       ↓
Create / Update Profile
       ↓
Display Extracted Information
       ↓
User Confirms / Corrects
       ↓
Profile Becomes Available
       ↓
Eligibility Engine
```

Never silently trust OCR output.

The user must be able to review and correct extracted information.

---

# PART 7 — GOVERNMENT SCHEME ELIGIBILITY ENGINE

This is the core application feature.

Do NOT make an LLM freely guess whether a user is eligible.

Use a structured rule-based eligibility engine.

Each scheme should contain structured rules.

Example:

```text
Scheme:
Example Government Scheme

Eligibility:

Age:
18–60

Annual Income:
≤ ₹2,50,000

State:
Tamil Nadu

Occupation:
Student / Worker

Category:
Specific categories

Required Documents:
Income Certificate
Residence Certificate
```

The engine evaluates each condition individually.

```text
USER PROFILE
      +
SCHEME RULES
      ↓
RULE EVALUATION
      ↓
CONDITION RESULTS
      ↓
OVERALL RESULT
      ↓
EXPLANATION
```

---

# PART 8 — ELIGIBILITY STATES

Use at least three states:

### ELIGIBLE

Known requirements are satisfied.

### POTENTIALLY ELIGIBLE

Available information suggests eligibility, but additional verification/documentation is required.

### NOT ELIGIBLE

One or more known requirements are not satisfied.

Every result must explain why.

Example:

```text
Scheme A

Status: NOT ELIGIBLE

✓ Age requirement satisfied
✓ State requirement satisfied
✓ Occupation requirement satisfied
✗ Income requirement failed

Your income:
₹3,20,000

Required:
≤ ₹2,50,000
```

---

# PART 9 — SCHEME RANKING

Display suitable schemes in descending order based on how closely the user matches the known eligibility criteria.

Do not present an arbitrary AI-generated score without an explainable basis.

Possible result:

```text
1. Scheme A
   Strong Match
   All known conditions satisfied

2. Scheme B
   Potential Match
   Additional document required

3. Scheme C
   Partial Match
   Income requirement not satisfied

4. Scheme D
   Not Eligible
   Age requirement not satisfied
```

Research suitable ranking methodologies before implementing one.

Clearly distinguish:

* Eligibility
* Suitability/relevance
* Missing information
* Failed requirements

---

# PART 10 — SCHEME DATABASE

Create a structured scheme model.

Each scheme should support:

```text
id
name
description
governmentDepartment
benefits
eligibilityRules
requiredDocuments
applicationProcess
officialSource
officialApplicationUrl
state
category
lastUpdated
status
```

Eligibility rules should be stored in a machine-readable structure so the engine can evaluate them.

Do not hard-code every scheme into Flutter code.

---

# PART 11 — OFFICIAL SOURCES

Government-scheme information is sensitive to changes.

Every scheme should ideally have:

* Official government source
* Official application URL
* Last updated date
* Government department
* Source/reference

The application should make it clear that:

> The application provides an eligibility indication based on the information supplied. Final eligibility is determined by the relevant government authority.

Research how existing systems handle outdated scheme information.

---

# PART 12 — ANONYMOUS LEGAL QUESTION SYSTEM

Normal users can ask questions anonymously.

Example:

```text
Anonymous User

"My application for a government scheme was rejected because
of the income requirement. What options do I have?"
```

The user's identity should not be displayed to lawyers unless the user deliberately provides identifying information in their question.

The question should be visible to:

```text
Original User
        +
Verified Lawyer A
Verified Lawyer B
Verified Lawyer C
...
```

It should NOT be visible to unrelated normal users.

---

# PART 13 — MULTIPLE LAWYER RESPONSES

Multiple verified lawyers can independently answer the same question.

Example:

```text
Anonymous Question
       |
   +---+---+---+
   |       |   |
Lawyer A Lawyer B Lawyer C
   |       |   |
Answer A Answer B Answer C
   \       |   /
       USER
```

The user sees all responses.

---

# PART 14 — LAWYER RESPONSE PRIVACY

This is a critical backend security requirement.

Lawyer A must NOT be able to retrieve or view Lawyer B's response.

Lawyer B must NOT be able to retrieve or view Lawyer A's response.

The database and backend authorization rules must enforce this.

Do not rely on merely hiding responses in the Flutter UI.

Research secure implementation patterns for this requirement.

---

# PART 15 — LAWYER REGISTRATION

Lawyers can create an account.

During registration they must provide necessary professional information and documents.

Possible information:

* Full name
* Email
* Phone
* Professional registration information
* Registration number
* State
* Specialization
* Experience
* Professional documents
* Identity/verification documents
* Contact information

The exact requirements should be configurable by the administrator.

---

# PART 16 — LAWYER VERIFICATION

After registration:

```text
Account Status:
ACTIVE

Lawyer Verification:
PENDING
```

The lawyer can enter the application but cannot use verified-lawyer interaction features.

Until approved, the lawyer cannot:

* Answer user questions
* Appear as a verified lawyer
* Interact with users as a verified legal specialist

Admin reviews the submitted documents.

Possible states:

```text
PENDING
VERIFIED
REJECTED
SUSPENDED
```

Only:

```text
VERIFIED
```

unlocks lawyer interaction.

---

# PART 17 — ADMIN

The administrator manages the platform.

Admin functions:

### User Management

* View users
* Suspend users
* Block users
* Manage account status

### Lawyer Management

* View applications
* Review documents
* Approve lawyers
* Reject lawyers
* Request additional documents
* Suspend verification

### Scheme Management

* Add schemes
* Edit schemes
* Deactivate schemes
* Add eligibility rules
* Add benefits
* Add required documents
* Add application procedures
* Add official sources
* Update schemes

### Moderation

* Review reported questions
* Review reported answers
* Handle abuse
* Handle inappropriate content

---

# PART 18 — LAWYER DIRECTORY

Provide a separate "Find a Lawyer" section.

Only verified lawyers should appear as verified professionals.

Lawyer profiles can contain:

```text
Name
Verified status
Specialization
Experience
Location
Languages
Professional information
Contact options
```

Allow searching/filtering by:

* Specialization
* Location
* Language
* Experience
* Availability

Research suitable lawyer-directory UX patterns.

---

# PART 19 — DIRECT CONTACT

Users should be able to contact lawyers directly.

Possible methods:

* Phone
* Email
* In-app communication
* Other appropriate contact mechanisms

Lawyers may also provide contact information in their answers.

Research privacy, spam prevention and platform safety considerations.

---

# PART 20 — RECOMMENDED TECH STACK

The mobile application MUST use:

## Frontend

**Flutter + Dart**

Use:

* Flutter
* Dart
* Material 3
* Responsive layouts
* Clean architecture
* Feature-based project organization

Research and recommend whether to use:

* Riverpod
* Bloc
* Provider

Choose ONE state-management approach and explain why.

Prefer a maintainable architecture rather than putting all logic inside widgets.

---

# PART 21 — BACKEND

For the first implementation, use **Firebase** unless research indicates a strong reason not to.

Potential Firebase services:

### Firebase Authentication

For:

* Normal users
* Lawyers
* Admins

### Cloud Firestore

For:

* Users
* Profiles
* Lawyers
* Verification status
* Schemes
* Eligibility rules
* Questions
* Answers
* Reports
* Notifications

### Firebase Storage

For:

* User certificates
* Lawyer verification documents
* Profile images where required

### Firebase Cloud Functions

For:

* Secure backend logic
* Eligibility processing where appropriate
* Document processing
* Notifications
* Administrative operations
* Sensitive authorization logic

### Firebase Cloud Messaging

For:

* Lawyer question notifications
* User answer notifications
* Verification updates
* Other application notifications

---

# PART 22 — AI / DOCUMENT PROCESSING

Research and recommend the best practical architecture for certificate extraction.

Possible technologies may include:

* Google ML Kit
* Google Cloud Vision
* Firebase-compatible document processing
* Gemini APIs
* Other document AI/OCR services

Do NOT automatically select one.

First compare:

* Accuracy
* Cost
* Free-tier availability
* Privacy
* Indian document support
* Tamil/English support
* Offline capability
* Security
* Ease of Flutter integration

Then recommend one architecture.

The extracted data must be validated by the user.

---

# PART 23 — AI USAGE

AI should be an assistance layer, not the authoritative eligibility decision-maker.

AI may be used for:

* OCR/document understanding
* Extracting fields from certificates
* Explaining eligibility
* Simplifying government language
* Categorizing questions
* Helping administrators enter scheme information
* Semantic search
* Natural-language interaction

The actual eligibility engine should primarily use:

```text
Structured Rules
       ↓
Deterministic Evaluation
       ↓
Explainable Result
```

AI can explain the result but should not silently override the structured rule engine.

---

# PART 24 — DATABASE STRUCTURE

Design a proper database.

At minimum consider:

```text
users
user_profiles
user_documents

lawyer_profiles
lawyer_verifications

schemes
eligibility_rules
scheme_documents

questions
lawyer_answers

notifications
reports

admin_actions
```

Design relationships carefully.

Research whether Firestore should use:

* Subcollections
* Top-level collections
* References
* Denormalization

Choose based on security, querying and scalability.

---

# PART 25 — SECURITY

Security is extremely important because the app handles:

* Personal information
* Income information
* Government certificates
* Identity documents
* Lawyer verification documents
* Legal questions

Implement:

* Firebase Authentication
* Firestore Security Rules
* Storage Security Rules
* Role-based authorization
* Status-based authorization
* Input validation
* File-type validation
* File-size restrictions
* Secure document access
* Audit logs for admin actions
* Rate limiting where appropriate
* Report/block mechanisms

Never expose private documents through publicly accessible storage URLs without appropriate authorization.

---

# PART 26 — ROLE AND PERMISSION MODEL

Use both:

## Role

```text
USER
LAWYER
ADMIN
```

and:

## Status

```text
Account:
ACTIVE
SUSPENDED
BLOCKED

Lawyer:
PENDING
VERIFIED
REJECTED
SUSPENDED
```

Example:

```text
USER + ACTIVE
→ normal user functionality

USER + ACTIVE + PROFILE_READY
→ personalized scheme matching

LAWYER + ACTIVE + PENDING
→ no lawyer interaction

LAWYER + ACTIVE + VERIFIED
→ lawyer interaction enabled

ADMIN + ACTIVE
→ administration
```

Enforce these permissions on the backend.

---

# PART 27 — FLUTTER APPLICATION STRUCTURE

Use a scalable feature-based structure.

Example:

```text
lib/
│
├── core/
│   ├── constants/
│   ├── theme/
│   ├── routing/
│   ├── errors/
│   ├── utils/
│   └── services/
│
├── features/
│
│   ├── auth/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── user/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── profile/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── documents/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── schemes/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── eligibility/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── questions/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── lawyers/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── lawyer_verification/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   ├── admin/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│
│   └── notifications/
│
└── main.dart
```

Adapt this structure if research suggests a better architecture.

Do not create unnecessary complexity.

---

# PART 28 — MAIN FLUTTER SCREENS

## Common

* Splash screen
* Login
* Registration
* Forgot password
* Account settings
* Notifications

## Normal User

* Home
* Profile
* Create Profile
* Upload Certificate
* Extracted Information Review
* Scheme Discovery
* Scheme Results
* Scheme Details
* Eligibility Explanation
* Ask Anonymous Question
* My Questions
* Lawyer Answers
* Find a Lawyer
* Lawyer Profile
* Contact Lawyer

## Lawyer

* Lawyer Dashboard
* Lawyer Profile
* Verification Status
* Upload Verification Documents
* Available Questions
* Question Details
* Submit Answer
* My Answers
* Contact Information
* Notifications

## Admin

Admin functionality may be implemented as a separate responsive web dashboard or an admin section depending on architecture.

Admin screens:

* Dashboard
* Users
* Lawyer Applications
* Lawyer Verification
* Scheme Management
* Eligibility Rules
* Questions
* Reports
* Settings
* Audit Logs

---

# PART 29 — HOME SCREEN CONCEPT

For normal users, the home screen should focus on:

```text
Welcome

Find Government Schemes
[Find Schemes]

Complete Your Profile
[Create Profile]

Upload Certificates
[Upload Documents]

Need Legal Help?
[Ask Anonymously]

Find a Verified Lawyer
[Find Lawyer]
```

Do not overwhelm users with unnecessary information.

---

# PART 30 — USER EXPERIENCE PRINCIPLE

The app should feel like a **citizen assistance platform**, not a complicated legal application.

The UX should be:

* Simple
* Accessible
* Clear
* Mobile-first
* Trustworthy
* Explainable
* Multilingual-ready

Design for users who may not understand legal or government terminology.

---

# PART 31 — DEVELOPMENT PHASES

Do NOT attempt to build everything at once.

Use phases.

### Phase 1

Project setup:

* Flutter
* Firebase
* Authentication
* Roles
* Navigation
* Theme

### Phase 2

Normal user:

* Profile
* Scheme database
* Scheme browsing
* Scheme details

### Phase 3

Eligibility engine:

* Rules
* Matching
* Ranking
* Explanation

### Phase 4

Lawyer:

* Registration
* Documents
* Verification
* Lawyer dashboard

### Phase 5

Anonymous Q&A:

* Questions
* Verified lawyer access
* Answers
* Privacy/security

### Phase 6

Lawyer directory:

* Profiles
* Search
* Filters
* Contact

### Phase 7

Document intelligence:

* OCR
* Extraction
* Profile generation
* User confirmation

### Phase 8

Admin:

* Dashboard
* Management
* Moderation
* Audit

### Phase 9

Testing and security:

* Unit tests
* Widget tests
* Integration tests
* Security-rule testing
* Role testing
* Error handling

---

# PART 32 — DEMO DATA

For the first working prototype, do NOT attempt to integrate hundreds or thousands of schemes.

Create a small set of realistic structured demo schemes.

For example:

```text
5–10 schemes
```

Each should have different combinations of:

* Income
* Age
* State
* Occupation
* Category
* Documents

This allows the eligibility engine to demonstrate:

* Full match
* Partial match
* Failed condition
* Missing information
* Document requirement

Clearly mark prototype/demo data where necessary.

---

# PART 33 — TEST SCENARIOS

The application must eventually test:

### Normal User

1. Register.
2. Browse without profile.
3. Create manual profile.
4. Run scheme matching.
5. See ranked results.
6. See failed conditions.
7. Upload a certificate.
8. Extract information.
9. Confirm information.
10. Run matching again.

### Lawyer

1. Register.
2. Upload professional documents.
3. Become PENDING.
4. Confirm that lawyer interaction is blocked.
5. Admin approves.
6. Lawyer becomes VERIFIED.
7. Lawyer can see questions.
8. Lawyer answers.
9. Confirm another lawyer cannot see the answer.
10. User receives the answer.

### Admin

1. Login.
2. See lawyer application.
3. View documents.
4. Approve/reject.
5. Manage schemes.
6. Modify eligibility rules.
7. Moderate content.

---

# PART 34 — RESEARCH OUTPUT REQUIRED BEFORE CODING

Before writing any code, produce a research report containing:

### 1. Existing Applications

List relevant products/projects.

### 2. Feature Comparison

Create a detailed comparison matrix.

### 3. Academic Research

List relevant papers/projects and explain their approaches.

### 4. Technical Approaches

Compare:

* Rule engines
* AI/LLM matching
* OCR
* Document AI
* Firebase
* Alternative backend architectures

### 5. Competitor Analysis

Explain where this project overlaps with existing products.

### 6. Differentiation

Identify the strongest defensible differences.

### 7. Recommended Architecture

Based on the research, recommend the architecture for this project.

### 8. Recommended Tech Stack

Explain every major technology choice.

### 9. Security Risks

Identify important security/privacy issues.

### 10. Development Risks

Identify technically difficult parts.

### 11. MVP Recommendation

Explain what should and should not be included in the first demo.

---

# PART 35 — IMPORTANT RESEARCH RULES

Use current information where possible.

Prioritize:

1. Official government sources
2. Official product websites
3. Official documentation
4. Academic papers
5. GitHub repositories
6. Reputable technology publications
7. App-store listings

Clearly distinguish:

* Verified facts
* Company claims
* Academic claims
* Your analysis
* Your recommendations

Do not invent features that you cannot verify.

Do not claim our idea is unique without evidence.

---

# PART 36 — STOP AFTER RESEARCH

THIS IS CRITICAL.

At this stage, **DO NOT WRITE ANY APPLICATION CODE.**

Do not create:

* Flutter files
* Dart files
* Firebase configuration
* Database code
* UI code
* Authentication code
* Firestore rules
* Project files

First complete the research and architecture analysis.

At the very end of the research response, output exactly:

**now proceed**

Do not begin development until I explicitly instruct you to proceed in a subsequent message.