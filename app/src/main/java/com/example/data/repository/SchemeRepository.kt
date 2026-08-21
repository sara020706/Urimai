package com.example.data.repository

import com.example.data.model.*

object SchemeRepository {

    val allSchemes: List<Scheme> = listOf(
        Scheme(
            id = "sch_vidya_lakshmi",
            name = "PM Vidya Lakshmi Higher Education Scholarship & Assistance",
            shortName = "PM Vidya Lakshmi",
            tamilName = "பிரதம மந்திரி வித்யா லக்ஷ்மி உயர்கல்வி உதவி",
            hindiName = "पीएम विद्या लक्ष्मी उच्च शिक्षा छात्रवृत्ति",
            category = SchemeCategory.EDUCATION,
            department = "Department of Higher Education, Ministry of Education",
            description = "Provides comprehensive financial support, course fee subsidies, and interest subvention for eligible students pursuing accredited undergraduate or postgraduate courses.",
            benefitHighlight = "Up to ₹50,000/year fee subsidy + maintenance grant",
            detailedBenefits = listOf(
                "Direct fee waiver credited to educational institution",
                "Annual maintenance allowance of ₹12,000 for course books & living expenses",
                "Full interest subvention during moratorium period for education loans"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "vl_student",
                    title = "Student Status",
                    conditionType = CriterionConditionType.STUDENT_STATUS,
                    targetValue = true,
                    requirementDisplay = "Must be currently enrolled as an active student",
                    explanationNote = "Applicants must be pursuing an accredited college degree or diploma.",
                    whyWeAskReason = "This scholarship is exclusively intended for actively enrolled students."
                ),
                EligibilityCriterion(
                    id = "vl_education",
                    title = "Education Level",
                    conditionType = CriterionConditionType.EDUCATION_LEVEL_IN,
                    targetValue = listOf("Undergraduate", "Postgraduate", "Diploma"),
                    requirementDisplay = "Pursuing Diploma, Undergraduate, or Postgraduate degree",
                    explanationNote = "Scheme covers post-secondary higher education qualifications.",
                    whyWeAskReason = "Helps verify enrollment in approved collegiate coursework."
                ),
                EligibilityCriterion(
                    id = "vl_age",
                    title = "Age Limit",
                    conditionType = CriterionConditionType.MAX_AGE,
                    targetValue = 28,
                    requirementDisplay = "Age between 17 and 28 years",
                    explanationNote = "Candidate must not exceed 28 years at time of application.",
                    whyWeAskReason = "Age limit set for standard college degree timelines."
                ),
                EligibilityCriterion(
                    id = "vl_income",
                    title = "Annual Family Income",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 250000L,
                    requirementDisplay = "Family income ≤ ₹2,50,000 / year",
                    explanationNote = "Aimed at supporting students from economically weaker households.",
                    whyWeAskReason = "Government scholarships prioritize students based on economic thresholds."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "Used for biometric KYC verification"
                ),
                SchemeDocument(
                    id = "doc_bonafide",
                    name = "Student ID / Bonafide Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "Issued by principal or registrar of current college"
                ),
                SchemeDocument(
                    id = "doc_marksheet",
                    name = "10th / 12th Marksheet",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "Proof of qualifying exam completion"
                ),
                SchemeDocument(
                    id = "doc_income_cert",
                    name = "Income Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Issued by Tahsildar / Revenue Authority (valid within 1 year)"
                ),
                SchemeDocument(
                    id = "doc_bank_passbook",
                    name = "Bank Account Passbook",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "Aadhaar-seeded bank account for Direct Benefit Transfer (DBT)"
                )
            ),
            officialSourceLabel = "Official government scheme portal (Prototype reference)",
            sourceUrl = "https://www.vidyalakshmi.co.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_pm_kisan",
            name = "PM-KISAN (Pradhan Mantri Kisan Samman Nidhi)",
            shortName = "PM-KISAN Samman",
            tamilName = "பிரதம மந்திரி கிசான் சம்மான் நிதி",
            hindiName = "पीएम-किसान सम्मान निधि योजना",
            category = SchemeCategory.AGRICULTURE,
            department = "Department of Agriculture & Farmers Welfare",
            description = "Provides income support to all landholding farmer families across India to supplement financial needs for agricultural inputs and domestic expenses.",
            benefitHighlight = "₹6,000 per year in three 4-monthly installments of ₹2,000",
            detailedBenefits = listOf(
                "Direct Benefit Transfer (DBT) directly into bank account",
                "Guaranteed 3 equal tranches every 4 months",
                "Linked with Kisan Credit Card (KCC) for low-interest agri-credit"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "pk_farmer",
                    title = "Farmer Status",
                    conditionType = CriterionConditionType.FARMER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Must be a practicing farmer with cultivable land",
                    explanationNote = "Valid landholding documents required in land registry records.",
                    whyWeAskReason = "Scheme exclusively benefits landholding farmer families."
                ),
                EligibilityCriterion(
                    id = "pk_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 350000L,
                    requirementDisplay = "Family income ≤ ₹3,50,000 / year",
                    explanationNote = "Constitutional post-holders and institutional landholders excluded.",
                    whyWeAskReason = "Ensures welfare reaches marginal and small agricultural households."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_patta",
                    name = "Land Ownership / Patta Document",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Chitta / Patta / 7/12 extract showing cultivable land"
                ),
                SchemeDocument(
                    id = "doc_bank_passbook",
                    name = "Bank Account Passbook",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "Must be NPCI mapped for Aadhaar Enabled Payment System"
                )
            ),
            officialSourceLabel = "Ministry of Agriculture & Farmers Welfare (Prototype reference)",
            sourceUrl = "https://pmkisan.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_pudhumai_penn",
            name = "Moovalur Ramamirtham Ammaiyar Pudhumai Penn Scheme",
            shortName = "Pudhumai Penn Scheme",
            tamilName = "புதுமைப் பெண் திட்டம் (மூவலூர் ராமாமிர்தம் அம்மையார்)",
            hindiName = "पुधुमई पेन योजना (तमिलनाडु)",
            category = SchemeCategory.EDUCATION,
            department = "Department of Social Welfare and Women Empowerment, Govt of Tamil Nadu",
            description = "Empowers female students from Tamil Nadu government schools pursuing higher education (degrees, engineering, medicine, diplomas) with monthly cash assistance.",
            benefitHighlight = "₹1,000 per month deposited directly into bank account until course completion",
            detailedBenefits = listOf(
                "Direct cash transfer of ₹1,000 on the 10th of every month",
                "Continues through all academic semesters without tuition cuts",
                "Combined eligibility with state free bus pass & laptop initiatives"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "pp_state",
                    title = "State of Residence",
                    conditionType = CriterionConditionType.STATE_MATCH,
                    targetValue = "Tamil Nadu",
                    requirementDisplay = "Resident of Tamil Nadu",
                    explanationNote = "This is a Tamil Nadu state government sponsored initiative.",
                    whyWeAskReason = "State-specific budget allocation limits enrollment to TN residents."
                ),
                EligibilityCriterion(
                    id = "pp_gender",
                    title = "Gender",
                    conditionType = CriterionConditionType.GENDER_MATCH,
                    targetValue = "Female",
                    requirementDisplay = "Must identify as Female",
                    explanationNote = "Designed to boost higher education retention among young women.",
                    whyWeAskReason = "Targeted girl-child and women empowerment program."
                ),
                EligibilityCriterion(
                    id = "pp_student",
                    title = "Student Status",
                    conditionType = CriterionConditionType.STUDENT_STATUS,
                    targetValue = true,
                    requirementDisplay = "Actively enrolled undergraduate or diploma student",
                    explanationNote = "Must be currently studying in a recognized higher education institute.",
                    whyWeAskReason = "Stipend is tied to continuous college enrollment."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_bonafide",
                    name = "Student ID / Bonafide Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "College enrollment letter with EMIS registration"
                ),
                SchemeDocument(
                    id = "doc_bank_passbook",
                    name = "Bank Account Passbook",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                )
            ),
            officialSourceLabel = "Govt of Tamil Nadu Social Welfare (Prototype reference)",
            sourceUrl = "https://pudhumaipenn.tn.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_pmegp",
            name = "Prime Minister's Employment Generation Programme (PMEGP)",
            shortName = "PMEGP Micro-Enterprise",
            tamilName = "பிரதம மந்திரி வேலைவாய்ப்பு உருவாக்கும் திட்டம்",
            hindiName = "प्रधानमंत्री रोजगार सृजन कार्यक्रम (PMEGP)",
            category = SchemeCategory.ENTREPRENEURSHIP,
            department = "Ministry of Micro, Small and Medium Enterprises (MSME)",
            description = "Credit-linked subsidy program to generate employment opportunities through establishment of micro-enterprises in non-farm sector.",
            benefitHighlight = "Government subsidy of 15% to 35% on project cost up to ₹50 Lakh",
            detailedBenefits = listOf(
                "Up to ₹50 Lakh loan for manufacturing units and ₹20 Lakh for service units",
                "Subsidy back-ended directly by KVIC through bank loans",
                "Mandatory EDP (Entrepreneurship Development Programme) skill training provided"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "pmegp_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 years or above",
                    explanationNote = "Individual must be legally capable of entering business loan contracts.",
                    whyWeAskReason = "Bank lending rules mandate minimum legal age."
                ),
                EligibilityCriterion(
                    id = "pmegp_education",
                    title = "Minimum Education",
                    conditionType = CriterionConditionType.EDUCATION_LEVEL_IN,
                    targetValue = listOf("10th Pass", "12th Pass", "Diploma", "Undergraduate", "Postgraduate", "Doctorate"),
                    requirementDisplay = "At least 10th Standard Pass (for projects above ₹10L)",
                    explanationNote = "Basic literacy required for enterprise project proposal management.",
                    whyWeAskReason = "Required by MSME guidelines for project underwriting."
                ),
                EligibilityCriterion(
                    id = "pmegp_biz",
                    title = "Business Aspirant / Ownership",
                    conditionType = CriterionConditionType.BUSINESS_OWNER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Aspiring entrepreneur or micro-business owner",
                    explanationNote = "New business projects only; existing units cannot re-apply under PMEGP.",
                    whyWeAskReason = "Program targets startup micro-enterprises."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_pan",
                    name = "PAN Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_marksheet",
                    name = "10th / 12th Marksheet",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_project_report",
                    name = "Detailed Project Report (DPR)",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Cost breakdown and estimated income statement of the venture"
                )
            ),
            officialSourceLabel = "KVIC / MSME Portal (Prototype reference)",
            sourceUrl = "https://www.kviconline.gov.in/pmegpeportal",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_pmkvy",
            name = "PMKVY 4.0 (Pradhan Mantri Kaushal Vikas Yojana)",
            shortName = "PMKVY Skill India",
            tamilName = "பிரதம மந்திரி திறன் மேம்பாட்டு திட்டம்",
            hindiName = "प्रधानमंत्री कौशल विकास योजना 4.0",
            category = SchemeCategory.SKILL_DEVELOPMENT,
            department = "Ministry of Skill Development & Entrepreneurship",
            description = "Skill certification scheme that enables youth to take up industry-relevant skill training for modern technologies like AI, robotics, drone piloting, and healthcare.",
            benefitHighlight = "100% Free NSQF accredited training + ₹8,000 stipend & placement support",
            detailedBenefits = listOf(
                "Government pays full course fees directly to training partners",
                "Nationally recognized NSDC certificate accepted across industries",
                "Accident insurance coverage for 3 years under PMSBY"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "pkvy_age",
                    title = "Age Range",
                    conditionType = CriterionConditionType.MAX_AGE,
                    targetValue = 35,
                    requirementDisplay = "Age between 15 and 35 years",
                    explanationNote = "Focuses on young job seekers and skill upskilling.",
                    whyWeAskReason = "Scheme targets working-age youth entry into formal economy."
                ),
                EligibilityCriterion(
                    id = "pkvy_employed",
                    title = "Employment Status",
                    conditionType = CriterionConditionType.EMPLOYED_STATUS,
                    targetValue = false,
                    requirementDisplay = "Unemployed or looking for skill transformation",
                    explanationNote = "Priority given to school dropouts and unemployed youth seeking placements.",
                    whyWeAskReason = "Measures whether candidate requires primary skilling support."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_bank_passbook",
                    name = "Bank Account Passbook",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_marksheet",
                    name = "10th / 12th Marksheet",
                    isMandatoryForEligibility = false,
                    stage = "Document Verification"
                )
            ),
            officialSourceLabel = "National Skill Development Corporation (Prototype reference)",
            sourceUrl = "https://www.pmkvyofficial.org",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_pmay_housing",
            name = "Pradhan Mantri Awas Yojana (PMAY-Gramin / Urban Housing for All)",
            shortName = "PMAY Pucca House Aid",
            tamilName = "பிரதம மந்திரி அனைவருக்கும் வீடு திட்டம்",
            hindiName = "प्रधानमंत्री आवास योजना (PMAY)",
            category = SchemeCategory.HOUSING,
            department = "Ministry of Housing and Urban Affairs / Ministry of Rural Development",
            description = "Provides direct financial assistance to eligible families with no pucca house to construct standard earthquake-resistant homes with toilet and clean cooking gas connections.",
            benefitHighlight = "Direct grant of ₹1.5 Lakh to ₹2.67 Lakh subsidy on housing",
            detailedBenefits = listOf(
                "Direct installment credit aligned with geo-tagged construction stages",
                "Additional 90 days of unskilled labor wages under MGNREGS",
                "Mandatory ₹12,000 assistance for toilet construction via Swachh Bharat"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "pmay_income",
                    title = "Annual Household Income",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 300000L,
                    requirementDisplay = "Family income ≤ ₹3,00,000 / year (EWS/LIG category)",
                    explanationNote = "Applicant family must not own any existing pucca house in India.",
                    whyWeAskReason = "Welfare housing is restricted to low-income and homeless households."
                ),
                EligibilityCriterion(
                    id = "pmay_family",
                    title = "Family Size",
                    conditionType = CriterionConditionType.FAMILY_SIZE_MIN,
                    targetValue = 2,
                    requirementDisplay = "Household family size ≥ 2 members",
                    explanationNote = "Housing benefits target multi-member family units.",
                    whyWeAskReason = "Verifies family household composition."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_income_cert",
                    name = "Income Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Issued by revenue department"
                ),
                SchemeDocument(
                    id = "doc_land_noc",
                    name = "Land Ownership / NOC Document",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Proof of land holding or allottee site"
                ),
                SchemeDocument(
                    id = "doc_bank_passbook",
                    name = "Bank Account Passbook",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                )
            ),
            officialSourceLabel = "Ministry of Housing & Urban Affairs (Prototype reference)",
            sourceUrl = "https://pmaymis.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_standup_india",
            name = "Stand-Up India Scheme for Women & SC/ST Entrepreneurs",
            shortName = "Stand-Up India",
            tamilName = "ஸ்டாண்ட்-அப் இந்தியா தொழில் கடன் திட்டம்",
            hindiName = "स्टैंड-अप इंडिया योजना",
            category = SchemeCategory.ENTREPRENEURSHIP,
            department = "Department of Financial Services, Ministry of Finance",
            description = "Facilitates bank loans between ₹10 Lakh and ₹1 Crore to at least one SC or ST borrower and at least one woman borrower per bank branch for setting up greenfield enterprises.",
            benefitHighlight = "Composite term loan & working capital between ₹10 Lakh and ₹1 Crore",
            detailedBenefits = listOf(
                "Low margin money requirement (up to 15% with state convergence)",
                "Handholding support from SIDBI and National Credit Guarantee Trustee Company",
                "Repayment period of up to 7 years with a 18-month moratorium"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "su_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 years or above",
                    explanationNote = "Required for commercial banking agreements.",
                    whyWeAskReason = "Statutory age for commercial credit facilities."
                ),
                EligibilityCriterion(
                    id = "su_social_gender",
                    title = "Target Demographic",
                    conditionType = CriterionConditionType.SOCIAL_CATEGORY_IN,
                    targetValue = listOf("SC", "ST"),
                    requirementDisplay = "Must be a Woman entrepreneur OR belonging to SC/ST category",
                    explanationNote = "Specially mandated to promote financial inclusion in greenfield enterprises.",
                    whyWeAskReason = "Parliamentary statutory guidelines reserve these loans for women and SC/ST founders."
                ),
                EligibilityCriterion(
                    id = "su_biz",
                    title = "Greenfield Enterprise",
                    conditionType = CriterionConditionType.BUSINESS_OWNER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Setting up a first-time greenfield business venture",
                    explanationNote = "Borrower must hold at least 51% shareholding in the enterprise.",
                    whyWeAskReason = "Commercial bank requirement for enterprise disbursement."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_pan",
                    name = "PAN Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_community_cert",
                    name = "Community / Caste Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Required if applying under SC/ST quota category"
                ),
                SchemeDocument(
                    id = "doc_project_report",
                    name = "Detailed Project Report (DPR)",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification"
                )
            ),
            officialSourceLabel = "Stand-Up India / SIDBI (Prototype reference)",
            sourceUrl = "https://www.standupmitra.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_tn_fellowship",
            name = "Tamil Nadu Chief Minister's Fellowship Programme (TNCMFP)",
            shortName = "TN CM Fellowship",
            tamilName = "தமிழ்நாடு முதலமைச்சரின் ஆய்வு உதவித்தொகைத் திட்டம்",
            hindiName = "तमिलनाडु मुख्यमंत्री फेलोशिप कार्यक्रम",
            category = SchemeCategory.EMPLOYMENT,
            department = "Special Programme Implementation Department, Govt of Tamil Nadu",
            description = "Prestigious 2-year public policy fellowship placing skilled young professionals with district administrators and state government departments.",
            benefitHighlight = "₹65,000 per month stipend + ₹10,000 monthly travel & housing allowance",
            detailedBenefits = listOf(
                "Direct mentorship under District Collectors and Principal Secretaries",
                "Academic certification from leading partner university (BIM Trichy)",
                "Full exposure to policy implementation and civic technology rollout"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "tnf_state",
                    title = "State Connection / Tamil Proficiency",
                    conditionType = CriterionConditionType.STATE_MATCH,
                    targetValue = "Tamil Nadu",
                    requirementDisplay = "Resident of Tamil Nadu / Fluent in Tamil & English",
                    explanationNote = "Field assignments are stationed across Tamil Nadu districts.",
                    whyWeAskReason = "District policy implementation requires working knowledge of Tamil."
                ),
                EligibilityCriterion(
                    id = "tnf_education",
                    title = "Education Qualification",
                    conditionType = CriterionConditionType.EDUCATION_LEVEL_IN,
                    targetValue = listOf("Postgraduate", "Doctorate"),
                    requirementDisplay = "Postgraduate Degree or 4-year Professional Degree with first class",
                    explanationNote = "Demands high analytical rigor for public policy research.",
                    whyWeAskReason = "Minimum requirement set by academic selection board."
                ),
                EligibilityCriterion(
                    id = "tnf_age",
                    title = "Age Limit",
                    conditionType = CriterionConditionType.MAX_AGE,
                    targetValue = 30,
                    requirementDisplay = "Age between 21 and 30 years",
                    explanationNote = "Young professional fellowship limit.",
                    whyWeAskReason = "Fellowship designed as early-to-mid career accelerator."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_pg_degree",
                    name = "Postgraduate / Professional Degree Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_resume",
                    name = "Statement of Purpose & Resume",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission",
                    tip = "Essay detailing public service motivation"
                )
            ),
            officialSourceLabel = "Govt of Tamil Nadu SPI Portal (Prototype reference)",
            sourceUrl = "https://tncmfp.tn.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_divyangjan_swavlamban",
            name = "Divyangjan Swavlamban Divyang Welfare & Mobility Support",
            shortName = "Divyang Welfare Support",
            tamilName = "மாற்றுத்திறனாளிகள் வாழ்வாதார உதவித் திட்டம்",
            hindiName = "दिव्यांगजन स्वावलंबन योजना",
            category = SchemeCategory.WELFARE,
            department = "Department of Empowerment of Persons with Disabilities, MSJE",
            description = "Provides concessional financial assistance, assistive motorized equipment subsidies, and skill empowerment grants for persons with disabilities.",
            benefitHighlight = "Up to ₹50,000 motorized aid subsidy + monthly pension allowance",
            detailedBenefits = listOf(
                "Free motorized tricycle / braille kits / hearing aid distribution",
                "Concessional 4% interest loans for self-employment livelihood projects",
                "Monthly maintenance support directly transferred via DBT"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "div_disability",
                    title = "Disability Status",
                    conditionType = CriterionConditionType.DISABILITY_STATUS,
                    targetValue = "Yes",
                    requirementDisplay = "Benchmark disability (40% or above)",
                    explanationNote = "Valid UDID card or medical certificate required.",
                    whyWeAskReason = "Statutory benefit specifically allocated for disabled citizens."
                ),
                EligibilityCriterion(
                    id = "div_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 300000L,
                    requirementDisplay = "Annual income ≤ ₹3,00,000",
                    explanationNote = "Targeted towards vulnerable and low-income beneficiaries.",
                    whyWeAskReason = "Ensures priority assistance to disadvantaged households."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(
                    id = "doc_aadhaar",
                    name = "Aadhaar Card",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                ),
                SchemeDocument(
                    id = "doc_udid",
                    name = "UDID / Disability Medical Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification",
                    tip = "Unique Disability ID card or certificate with 40%+ rating"
                ),
                SchemeDocument(
                    id = "doc_income_cert",
                    name = "Income Certificate",
                    isMandatoryForEligibility = true,
                    stage = "Document Verification"
                ),
                SchemeDocument(
                    id = "doc_bank_passbook",
                    name = "Bank Account Passbook",
                    isMandatoryForEligibility = true,
                    stage = "Application Submission"
                )
            ),
            officialSourceLabel = "National Divyangjan Finance Development Corp (Prototype reference)",
            sourceUrl = "https://www.nhfdc.nic.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_ayushman_bharat",
            name = "Ayushman Bharat Pradhan Mantri Jan Arogya Yojana (AB PM-JAY)",
            shortName = "Ayushman Bharat PM-JAY",
            tamilName = "ஆயுஷ்மான் பாரத் பிரதம மந்திரி ஜன் ஆரோக்ய திட்டம்",
            hindiName = "आयुष्मान भारत प्रधानमंत्री जन आरोग्य योजना",
            category = SchemeCategory.WELFARE,
            department = "National Health Authority, Ministry of Health & Family Welfare",
            description = "Provides free cashless health insurance coverage for secondary and tertiary hospitalization to economically vulnerable families.",
            benefitHighlight = "₹5 Lakh per family per year cashless health cover",
            detailedBenefits = listOf(
                "Cashless treatment at empanelled public and private hospitals",
                "Covers pre-existing conditions from day one",
                "No cap on family size or age"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "ab_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 250000L,
                    requirementDisplay = "Family income ≤ ₹2,50,000 / year",
                    explanationNote = "Targeted at economically vulnerable households per SECC-2011 deprivation criteria.",
                    whyWeAskReason = "Determines inclusion under the deprivation-based beneficiary list."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_ration_card", name = "Ration Card", isMandatoryForEligibility = true, stage = "Document Verification", tip = "Used to verify household deprivation category")
            ),
            officialSourceLabel = "National Health Authority (Prototype reference)",
            sourceUrl = "https://pmjay.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_ujjwala_yojana",
            name = "Pradhan Mantri Ujjwala Yojana (PMUY) LPG Connection Scheme",
            shortName = "PM Ujjwala Yojana",
            tamilName = "பிரதம மந்திரி உஜ்ஜ்வலா திட்டம்",
            hindiName = "प्रधानमंत्री उज्ज्वला योजना",
            category = SchemeCategory.WELFARE,
            department = "Ministry of Petroleum and Natural Gas",
            description = "Provides free LPG gas connections to women from below-poverty-line households to promote clean cooking fuel access.",
            benefitHighlight = "Free LPG connection with first refill & stove support",
            detailedBenefits = listOf(
                "Zero-cost security deposit for new LPG connection",
                "Financial assistance for first cylinder refill and hotplate",
                "Reduces indoor air pollution from traditional cooking fuels"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "puy_gender",
                    title = "Gender",
                    conditionType = CriterionConditionType.GENDER_MATCH,
                    targetValue = "Female",
                    requirementDisplay = "Applicant must be a woman aged 18 or above",
                    explanationNote = "Connections are issued in the name of the adult woman of the household.",
                    whyWeAskReason = "Scheme is designed to empower women as primary beneficiaries."
                ),
                EligibilityCriterion(
                    id = "puy_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 200000L,
                    requirementDisplay = "Family income ≤ ₹2,00,000 / year (BPL households)",
                    explanationNote = "Restricted to households below the poverty line as per state records.",
                    whyWeAskReason = "Ensures the subsidy reaches genuinely low-income households."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_ration_card", name = "Ration Card", isMandatoryForEligibility = true, stage = "Application Submission", tip = "BPL ration card confirms income category"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Petroleum and Natural Gas (Prototype reference)",
            sourceUrl = "https://pmuy.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_sukanya_samriddhi",
            name = "Sukanya Samriddhi Yojana (SSY) Girl Child Savings Scheme",
            shortName = "Sukanya Samriddhi Yojana",
            tamilName = "சுகன்யா சம்ரிதி திட்டம்",
            hindiName = "सुकन्या समृद्धि योजना",
            category = SchemeCategory.FINANCIAL_ASSISTANCE,
            department = "Department of Economic Affairs, Ministry of Finance",
            description = "A small savings scheme for the girl child, offering high fixed interest returns to support future education and marriage expenses.",
            benefitHighlight = "High government-fixed interest rate on deposits until maturity",
            detailedBenefits = listOf(
                "Deposits qualify for tax deduction under Section 80C",
                "Partial withdrawal allowed for higher education after age 18",
                "Account matures 21 years from opening or on marriage after 18"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "ssy_gender",
                    title = "Gender & Age",
                    conditionType = CriterionConditionType.GENDER_MATCH,
                    targetValue = "Female",
                    requirementDisplay = "Girl child below 10 years of age",
                    explanationNote = "Account must be opened by a parent/guardian for a female child under 10.",
                    whyWeAskReason = "Scheme is exclusively for the welfare of the girl child."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_birth_cert", name = "Birth Certificate", isMandatoryForEligibility = true, stage = "Application Submission", tip = "Proof of girl child's date of birth"),
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_guardian_id", name = "Guardian ID Proof", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "India Post / Authorized Banks (Prototype reference)",
            sourceUrl = "https://www.indiapost.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_nsp_central_scholarship",
            name = "National Means-cum-Merit Scholarship (NMMSS)",
            shortName = "NMMSS Merit Scholarship",
            tamilName = "தேசிய தகுதி அடிப்படை உதவித்தொகை",
            hindiName = "राष्ट्रीय साधन सह मेधा छात्रवृत्ति",
            category = SchemeCategory.EDUCATION,
            department = "Department of School Education & Literacy, Ministry of Education",
            description = "Provides scholarships to meritorious students from economically weaker sections to reduce dropout rates at the secondary school stage.",
            benefitHighlight = "₹12,000 per year until Class 12",
            detailedBenefits = listOf(
                "Direct annual scholarship credited to student bank account",
                "Awarded based on a qualifying state-level merit examination",
                "Renewable each year subject to minimum attendance and marks"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "nmmss_student",
                    title = "Student Status",
                    conditionType = CriterionConditionType.STUDENT_STATUS,
                    targetValue = true,
                    requirementDisplay = "Enrolled in Class 9 to 12 in a government school",
                    explanationNote = "Applicant must be a regular student in a state government or aided school.",
                    whyWeAskReason = "Scholarship targets continuation of secondary schooling."
                ),
                EligibilityCriterion(
                    id = "nmmss_income",
                    title = "Annual Family Income",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 150000L,
                    requirementDisplay = "Parental income ≤ ₹1,50,000 / year",
                    explanationNote = "Restricted to economically weaker section households.",
                    whyWeAskReason = "Ensures scholarship funds prioritize low-income meritorious students."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_bonafide", name = "Student ID / Bonafide Certificate", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_income_cert", name = "Income Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "National Scholarship Portal (Prototype reference)",
            sourceUrl = "https://scholarships.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_mudra_loan",
            name = "Pradhan Mantri MUDRA Yojana (PMMY) Micro-Credit Scheme",
            shortName = "PM MUDRA Yojana",
            tamilName = "பிரதம மந்திரி முத்ரா திட்டம்",
            hindiName = "प्रधानमंत्री मुद्रा योजना",
            category = SchemeCategory.ENTREPRENEURSHIP,
            department = "Department of Financial Services, Ministry of Finance",
            description = "Provides collateral-free micro-loans up to ₹20 Lakh to non-corporate, non-farm small and micro enterprises.",
            benefitHighlight = "Collateral-free loans up to ₹20 Lakh (Shishu / Kishor / Tarun / Tarun Plus)",
            detailedBenefits = listOf(
                "No collateral or third-party guarantee required",
                "Loans categorized by business growth stage for flexible ticket sizes",
                "Available through banks, NBFCs, and microfinance institutions"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "mudra_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 years or above",
                    explanationNote = "Standard legal age requirement for loan agreements.",
                    whyWeAskReason = "Statutory requirement for entering credit contracts."
                ),
                EligibilityCriterion(
                    id = "mudra_biz",
                    title = "Non-Farm Micro Enterprise",
                    conditionType = CriterionConditionType.BUSINESS_OWNER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Owns or plans to start a non-farm micro/small enterprise",
                    explanationNote = "Covers manufacturing, trading, and service sector micro-units.",
                    whyWeAskReason = "Scheme is designed specifically for small non-farm businesses."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_pan", name = "PAN Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_project_report", name = "Detailed Project Report (DPR)", isMandatoryForEligibility = true, stage = "Document Verification")
            ),
            officialSourceLabel = "MUDRA / Department of Financial Services (Prototype reference)",
            sourceUrl = "https://www.mudra.org.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_atal_pension",
            name = "Atal Pension Yojana (APY)",
            shortName = "Atal Pension Yojana",
            tamilName = "அடல் ஓய்வூதியத் திட்டம்",
            hindiName = "अटल पेंशन योजना",
            category = SchemeCategory.FINANCIAL_ASSISTANCE,
            department = "Pension Fund Regulatory and Development Authority (PFRDA)",
            description = "A voluntary, government-backed pension scheme aimed at unorganized sector workers, guaranteeing a fixed monthly pension after age 60.",
            benefitHighlight = "Guaranteed monthly pension of ₹1,000 to ₹5,000 after age 60",
            detailedBenefits = listOf(
                "Fixed monthly contribution based on age of joining and chosen pension slab",
                "Government co-contribution for eligible early subscribers",
                "Spouse continuation and nominee corpus payout benefits"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "apy_age",
                    title = "Age Range",
                    conditionType = CriterionConditionType.MAX_AGE,
                    targetValue = 40,
                    requirementDisplay = "Age between 18 and 40 years",
                    explanationNote = "Enrollment window ensures at least 20 years of contribution before pension starts.",
                    whyWeAskReason = "Pension payout structure is based on years of contribution."
                ),
                EligibilityCriterion(
                    id = "apy_employed",
                    title = "Unorganized Sector Worker",
                    conditionType = CriterionConditionType.EMPLOYED_STATUS,
                    targetValue = false,
                    requirementDisplay = "Not covered under any statutory social security scheme",
                    explanationNote = "Targeted at workers without formal employer-provided pension coverage.",
                    whyWeAskReason = "Scheme complements the unorganized sector's lack of pension access."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission", tip = "Contributions are auto-debited monthly")
            ),
            officialSourceLabel = "PFRDA (Prototype reference)",
            sourceUrl = "https://npscra.nsdl.co.in/nsdl/scheme-details.php",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_janani_suraksha",
            name = "Janani Suraksha Yojana (JSY) Maternal Health Scheme",
            shortName = "Janani Suraksha Yojana",
            tamilName = "ஜனனி சுரக்ஷா திட்டம்",
            hindiName = "जननी सुरक्षा योजना",
            category = SchemeCategory.WELFARE,
            department = "National Health Mission, Ministry of Health & Family Welfare",
            description = "Provides cash assistance to pregnant women to promote institutional delivery and reduce maternal and infant mortality.",
            benefitHighlight = "Cash assistance of ₹700 to ₹1,400 for institutional delivery",
            detailedBenefits = listOf(
                "Direct cash benefit linked to delivery at a government health facility",
                "Free antenatal and postnatal check-ups at empanelled centers",
                "Additional transport assistance for rural beneficiaries"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "jsy_gender",
                    title = "Gender",
                    conditionType = CriterionConditionType.GENDER_MATCH,
                    targetValue = "Female",
                    requirementDisplay = "Pregnant woman opting for institutional delivery",
                    explanationNote = "Benefit is tied to maternal health during pregnancy and delivery.",
                    whyWeAskReason = "Scheme is exclusively for maternal welfare."
                ),
                EligibilityCriterion(
                    id = "jsy_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 300000L,
                    requirementDisplay = "Family income ≤ ₹3,00,000 / year (higher priority in low-performing states)",
                    explanationNote = "Priority given to BPL and low-income households in high-focus states.",
                    whyWeAskReason = "Ensures maternal health support reaches vulnerable households first."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_jsy_card", name = "Mother and Child Protection (MCP) Card", isMandatoryForEligibility = true, stage = "Document Verification", tip = "Issued at first antenatal registration"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "National Health Mission (Prototype reference)",
            sourceUrl = "https://nhm.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_nsap_old_age",
            name = "National Social Assistance Programme - Old Age Pension (IGNOAPS)",
            shortName = "IGNOAPS Old Age Pension",
            tamilName = "இந்திரா காந்தி முதுமை ஓய்வூதியத் திட்டம்",
            hindiName = "इंदिरा गांधी राष्ट्रीय वृद्धावस्था पेंशन योजना",
            category = SchemeCategory.WELFARE,
            department = "Ministry of Rural Development",
            description = "Provides a monthly pension to elderly citizens living below the poverty line to ensure basic financial security in old age.",
            benefitHighlight = "Monthly pension of ₹200 to ₹500 (state top-ups may apply)",
            detailedBenefits = listOf(
                "Direct monthly pension transfer via bank/post office account",
                "Combined with state government pension top-up schemes",
                "No repayment or contribution required"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "ignoaps_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 60,
                    requirementDisplay = "Age 60 years or above",
                    explanationNote = "Pension eligibility begins strictly from age 60 onward.",
                    whyWeAskReason = "Scheme targets senior citizens in old age."
                ),
                EligibilityCriterion(
                    id = "ignoaps_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 100000L,
                    requirementDisplay = "Family income ≤ ₹1,00,000 / year (BPL households)",
                    explanationNote = "Restricted to households below the poverty line.",
                    whyWeAskReason = "Ensures pension support prioritizes the poorest elderly citizens."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_age_proof", name = "Age Proof Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_ration_card", name = "Ration Card", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Rural Development (Prototype reference)",
            sourceUrl = "https://nsap.nic.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_pmfby_crop_insurance",
            name = "Pradhan Mantri Fasal Bima Yojana (PMFBY) Crop Insurance",
            shortName = "PM Fasal Bima Yojana",
            tamilName = "பிரதம மந்திரி பயிர் காப்பீட்டு திட்டம்",
            hindiName = "प्रधानमंत्री फसल बीमा योजना",
            category = SchemeCategory.AGRICULTURE,
            department = "Department of Agriculture & Farmers Welfare",
            description = "Provides comprehensive crop insurance coverage against yield losses due to natural calamities, pests, and diseases at a low farmer premium.",
            benefitHighlight = "Low uniform premium (1.5%-5% of sum insured) with full claim coverage",
            detailedBenefits = listOf(
                "Covers pre-sowing to post-harvest losses due to natural risks",
                "Use of technology (drones, satellite imaging) for quick claim assessment",
                "Direct claim settlement into farmer's bank account"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "pmfby_farmer",
                    title = "Farmer Status",
                    conditionType = CriterionConditionType.FARMER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Must be a farmer growing notified crops (owner or tenant)",
                    explanationNote = "Both loanee and non-loanee farmers cultivating notified crops are eligible.",
                    whyWeAskReason = "Insurance coverage is tied to active crop cultivation."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_patta", name = "Land Ownership / Patta Document", isMandatoryForEligibility = true, stage = "Document Verification", tip = "Or tenant farmer agreement where applicable"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_sowing_cert", name = "Sowing Declaration Certificate", isMandatoryForEligibility = false, stage = "Document Verification")
            ),
            officialSourceLabel = "Department of Agriculture & Farmers Welfare (Prototype reference)",
            sourceUrl = "https://pmfby.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_kcc_credit_card",
            name = "Kisan Credit Card (KCC) Scheme",
            shortName = "Kisan Credit Card",
            tamilName = "விவசாயிகள் கடன் அட்டைத் திட்டம்",
            hindiName = "किसान क्रेडिट कार्ड योजना",
            category = SchemeCategory.AGRICULTURE,
            department = "Department of Agriculture & Farmers Welfare / NABARD",
            description = "Provides farmers with timely access to short-term credit for cultivation expenses, farm equipment, and allied agricultural activities at concessional interest rates.",
            benefitHighlight = "Concessional credit up to ₹3 Lakh at 4% effective interest (with prompt repayment)",
            detailedBenefits = listOf(
                "Flexible withdrawal and repayment tied to crop cycles",
                "Interest subvention of 3% for timely repayment",
                "Also covers post-harvest expenses and allied activities like dairy/fisheries"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "kcc_farmer",
                    title = "Farmer Status",
                    conditionType = CriterionConditionType.FARMER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Individual or joint farmer, tenant farmer, or sharecropper",
                    explanationNote = "Covers a wide range of cultivating farmer categories.",
                    whyWeAskReason = "Credit facility is designed exclusively for active cultivators."
                ),
                EligibilityCriterion(
                    id = "kcc_age",
                    title = "Age Range",
                    conditionType = CriterionConditionType.MAX_AGE,
                    targetValue = 75,
                    requirementDisplay = "Age between 18 and 75 years",
                    explanationNote = "Applicants above 60 may require a co-applicant legal heir.",
                    whyWeAskReason = "Bank credit risk policy requires an age ceiling with co-applicant support."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_patta", name = "Land Ownership / Patta Document", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "NABARD / Participating Banks (Prototype reference)",
            sourceUrl = "https://www.nabard.org",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_mgnrega",
            name = "Mahatma Gandhi National Rural Employment Guarantee Act (MGNREGA)",
            shortName = "MGNREGA Rural Employment",
            tamilName = "மகாத்மா காந்தி தேசிய ஊரக வேலைவாய்ப்பு உறுதித் திட்டம்",
            hindiName = "महात्मा गांधी राष्ट्रीय ग्रामीण रोजगार गारंटी योजना",
            category = SchemeCategory.EMPLOYMENT,
            department = "Ministry of Rural Development",
            description = "Guarantees 100 days of wage employment per financial year to every rural household whose adult members volunteer for unskilled manual work.",
            benefitHighlight = "Guaranteed 100 days/year of wage employment at notified rural wage rate",
            detailedBenefits = listOf(
                "Wages credited directly to bank/post office account within 15 days",
                "Unemployment allowance if work not provided within 15 days of demand",
                "Work sites include water conservation, rural roads, and afforestation projects"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "mgnrega_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 years or above",
                    explanationNote = "Only adult household members can register for job cards.",
                    whyWeAskReason = "Statutory minimum working age for manual labor employment."
                ),
                EligibilityCriterion(
                    id = "mgnrega_employed",
                    title = "Willing to Undertake Unskilled Manual Work",
                    conditionType = CriterionConditionType.EMPLOYED_STATUS,
                    targetValue = false,
                    requirementDisplay = "Rural household member seeking unskilled manual work",
                    explanationNote = "Scheme applies to households seeking casual manual labor employment.",
                    whyWeAskReason = "Distinguishes applicants seeking guaranteed rural employment."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_job_card_photo", name = "Passport Size Photograph", isMandatoryForEligibility = true, stage = "Application Submission", tip = "For issuance of the MGNREGA Job Card"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Rural Development (Prototype reference)",
            sourceUrl = "https://nrega.nic.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_ncs_rozgar_mela",
            name = "National Career Service (NCS) Employment Portal & Rozgar Mela",
            shortName = "NCS Employment Portal",
            tamilName = "தேசிய தொழில் சேவை போர்ட்டல்",
            hindiName = "राष्ट्रीय करियर सेवा पोर्टल",
            category = SchemeCategory.EMPLOYMENT,
            department = "Ministry of Labour & Employment",
            description = "A free online job-matching and career counselling platform connecting job seekers with employers, along with regular offline recruitment drives (Rozgar Melas).",
            benefitHighlight = "Free job matching, resume building, and career counselling services",
            detailedBenefits = listOf(
                "Access to verified job listings from private and government employers",
                "Free vocational guidance and career counselling sessions",
                "Priority access to district-level Rozgar Mela recruitment drives"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "ncs_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 15,
                    requirementDisplay = "Age 15 years or above",
                    explanationNote = "Open to all job seekers above minimum working age.",
                    whyWeAskReason = "Aligns with minimum legal working age guidelines."
                ),
                EligibilityCriterion(
                    id = "ncs_employed",
                    title = "Job Seeker Status",
                    conditionType = CriterionConditionType.EMPLOYED_STATUS,
                    targetValue = false,
                    requirementDisplay = "Actively seeking employment or career change",
                    explanationNote = "Platform is intended for individuals actively looking for work.",
                    whyWeAskReason = "Helps prioritize active job seekers on the portal."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_resume", name = "Statement of Purpose & Resume", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Labour & Employment (Prototype reference)",
            sourceUrl = "https://www.ncs.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_startup_india",
            name = "Startup India Seed Fund & Recognition Scheme",
            shortName = "Startup India Seed Fund",
            tamilName = "தொடக்க இந்தியா நிதி உதவித் திட்டம்",
            hindiName = "स्टार्टअप इंडिया सीड फंड योजना",
            category = SchemeCategory.ENTREPRENEURSHIP,
            department = "Department for Promotion of Industry and Internal Trade (DPIIT)",
            description = "Provides financial assistance and DPIIT recognition to early-stage startups for proof-of-concept, prototype development, and market entry.",
            benefitHighlight = "Seed funding up to ₹20 Lakh (grant) and ₹50 Lakh (debt/convertible)",
            detailedBenefits = listOf(
                "Tax exemptions for 3 consecutive years upon DPIIT recognition",
                "Fast-track patent examination with 80% fee rebate",
                "Access to government tenders without prior turnover/experience criteria"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "startup_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 years or above",
                    explanationNote = "Founders must be legal adults to register a company/LLP.",
                    whyWeAskReason = "Statutory requirement for business incorporation."
                ),
                EligibilityCriterion(
                    id = "startup_biz",
                    title = "Early-Stage Startup Founder",
                    conditionType = CriterionConditionType.BUSINESS_OWNER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Incorporated less than 2 years ago with innovative product/service",
                    explanationNote = "Entity must not be formed by splitting up an existing business.",
                    whyWeAskReason = "Scheme is reserved for genuinely early-stage innovative startups."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_pan", name = "PAN Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_incorporation_cert", name = "Certificate of Incorporation", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_project_report", name = "Detailed Project Report (DPR)", isMandatoryForEligibility = true, stage = "Document Verification")
            ),
            officialSourceLabel = "Startup India / DPIIT (Prototype reference)",
            sourceUrl = "https://www.startupindia.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_pmuy_svanidhi",
            name = "PM Street Vendor's AtmaNirbhar Nidhi (PM SVANidhi)",
            shortName = "PM SVANidhi",
            tamilName = "பிரதம மந்திரி தெருக் கடைவர் நிதி உதவித் திட்டம்",
            hindiName = "प्रधानमंत्री स्वनिधि योजना",
            category = SchemeCategory.ENTREPRENEURSHIP,
            department = "Ministry of Housing and Urban Affairs",
            description = "Provides affordable working capital loans to urban street vendors to resume and grow their livelihoods.",
            benefitHighlight = "Collateral-free loans starting at ₹10,000, scaling up to ₹50,000",
            detailedBenefits = listOf(
                "No collateral required for the loan amount",
                "7% annual interest subsidy on timely/early repayment",
                "Cashback incentive for adopting digital payment transactions"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "svanidhi_biz",
                    title = "Street Vendor Status",
                    conditionType = CriterionConditionType.BUSINESS_OWNER_STATUS,
                    targetValue = true,
                    requirementDisplay = "Vending in urban areas as of or before March 2020",
                    explanationNote = "Applicant must possess a Certificate of Vending or a Letter of Recommendation from the Urban Local Body.",
                    whyWeAskReason = "Scheme exclusively targets registered urban street vendors."
                ),
                EligibilityCriterion(
                    id = "svanidhi_age",
                    title = "Minimum Age",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 years or above",
                    explanationNote = "Legal age requirement to avail micro-credit facility.",
                    whyWeAskReason = "Statutory requirement for loan agreements."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_vending_cert", name = "Certificate of Vending / Vendor ID", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Housing and Urban Affairs (Prototype reference)",
            sourceUrl = "https://pmsvanidhi.mohua.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_deendayal_antyodaya",
            name = "Deendayal Antyodaya Yojana - National Urban Livelihoods Mission (DAY-NULM)",
            shortName = "DAY-NULM Urban Livelihoods",
            tamilName = "தீன்தயாள் அந்த்யோதயா தேசிய நகர்ப்புற வாழ்வாதாரத் திட்டம்",
            hindiName = "दीनदयाल अंत्योदय योजना - राष्ट्रीय शहरी आजीविका मिशन",
            category = SchemeCategory.SKILL_DEVELOPMENT,
            department = "Ministry of Housing and Urban Affairs",
            description = "Provides skill training, self-employment support, and shelter facilities for urban poor and homeless populations to build sustainable livelihoods.",
            benefitHighlight = "Free skill training + subsidized micro-enterprise loans for urban poor",
            detailedBenefits = listOf(
                "Market-relevant skill training with certification and placement linkage",
                "Interest subsidy on individual and group micro-enterprise loans",
                "Access to shelters and support services for urban homeless"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "nulm_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 200000L,
                    requirementDisplay = "Family income ≤ ₹2,00,000 / year (urban poor households)",
                    explanationNote = "Targeted at economically weaker urban households.",
                    whyWeAskReason = "Ensures livelihood support reaches genuinely disadvantaged urban residents."
                ),
                EligibilityCriterion(
                    id = "nulm_employed",
                    title = "Unemployed or Underemployed",
                    conditionType = CriterionConditionType.EMPLOYED_STATUS,
                    targetValue = false,
                    requirementDisplay = "Currently unemployed or in low-income informal work",
                    explanationNote = "Priority given to those without stable formal employment.",
                    whyWeAskReason = "Measures need for skilling and livelihood support."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_income_cert", name = "Income Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Housing and Urban Affairs (Prototype reference)",
            sourceUrl = "https://nulm.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_tn_marriage_assistance",
            name = "Tamil Nadu Moovalur Ramamirtham Ammaiyar Marriage Assistance Scheme",
            shortName = "TN Marriage Assistance",
            tamilName = "மூவலூர் ராமாமிர்தம் அம்மையார் திருமண உதவித் திட்டம்",
            hindiName = "तमिलनाडु विवाह सहायता योजना",
            category = SchemeCategory.WELFARE,
            department = "Department of Social Welfare and Women Empowerment, Govt of Tamil Nadu",
            description = "Provides one-time financial assistance for the marriage of daughters from poor families, orphan girls, and widow remarriages in Tamil Nadu.",
            benefitHighlight = "One-time cash assistance of ₹25,000 to ₹50,000 depending on category",
            detailedBenefits = listOf(
                "Higher assistance for orphan brides and widow remarriage cases",
                "Direct bank transfer disbursed before or after the marriage registration",
                "Additional gold coin incentive under certain state variants"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "tnma_state",
                    title = "State of Residence",
                    conditionType = CriterionConditionType.STATE_MATCH,
                    targetValue = "Tamil Nadu",
                    requirementDisplay = "Resident of Tamil Nadu",
                    explanationNote = "State government welfare scheme restricted to Tamil Nadu domiciles.",
                    whyWeAskReason = "Budget allocation is limited to Tamil Nadu residents."
                ),
                EligibilityCriterion(
                    id = "tnma_income",
                    title = "Annual Family Income",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 72000L,
                    requirementDisplay = "Family income ≤ ₹72,000 / year",
                    explanationNote = "Restricted to families below the state-defined poverty threshold.",
                    whyWeAskReason = "Ensures assistance is prioritized for the poorest families."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_marriage_cert", name = "Marriage Registration Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_income_cert", name = "Income Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Govt of Tamil Nadu Social Welfare (Prototype reference)",
            sourceUrl = "https://tnsocialwelfare.tn.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_tn_free_bus_pass",
            name = "Tamil Nadu Free Bus Travel Scheme for Women",
            shortName = "TN Free Bus Travel (Women)",
            tamilName = "தமிழ்நாடு பெண்களுக்கான இலவச பேருந்து பயணத் திட்டம்",
            hindiName = "तमिलनाडु महिला मुफ्त बस यात्रा योजना",
            category = SchemeCategory.WELFARE,
            department = "Transport Department, Govt of Tamil Nadu",
            description = "Provides free travel for women in ordinary/town bus services operated by state transport corporations across Tamil Nadu.",
            benefitHighlight = "100% free travel on government ordinary bus services",
            detailedBenefits = listOf(
                "Applicable across all districts on ordinary and town bus routes",
                "No separate pass application required; verified via Aadhaar-linked ID",
                "Significant monthly commuting cost savings for working women and students"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "tnbus_gender",
                    title = "Gender",
                    conditionType = CriterionConditionType.GENDER_MATCH,
                    targetValue = "Female",
                    requirementDisplay = "Must identify as Female",
                    explanationNote = "Scheme benefit is provided exclusively to women commuters.",
                    whyWeAskReason = "State welfare initiative targeted at women's mobility and safety."
                ),
                EligibilityCriterion(
                    id = "tnbus_state",
                    title = "State of Residence",
                    conditionType = CriterionConditionType.STATE_MATCH,
                    targetValue = "Tamil Nadu",
                    requirementDisplay = "Resident of Tamil Nadu",
                    explanationNote = "Applicable only within Tamil Nadu state transport corporation routes.",
                    whyWeAskReason = "State-funded scheme limited to Tamil Nadu residents and routes."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission", tip = "Used for identity verification while boarding")
            ),
            officialSourceLabel = "Govt of Tamil Nadu Transport Department (Prototype reference)",
            sourceUrl = "https://www.tn.gov.in/department/transport",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_tn_breakfast_scheme",
            name = "Tamil Nadu Chief Minister's Breakfast Scheme",
            shortName = "TN CM Breakfast Scheme",
            tamilName = "தமிழ்நாடு முதலமைச்சரின் காலை உணவுத் திட்டம்",
            hindiName = "तमिलनाडु मुख्यमंत्री नाश्ता योजना",
            category = SchemeCategory.EDUCATION,
            department = "School Education Department, Govt of Tamil Nadu",
            description = "Provides free nutritious breakfast to children studying in Tamil Nadu government primary schools to improve nutrition and school attendance.",
            benefitHighlight = "Free daily nutritious breakfast for eligible government school students",
            detailedBenefits = listOf(
                "Served daily before the start of school hours",
                "Menu curated by nutrition experts to reduce classroom hunger and dropout",
                "Complements the existing midday meal scheme"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "tnbreakfast_student",
                    title = "Student Status",
                    conditionType = CriterionConditionType.STUDENT_STATUS,
                    targetValue = true,
                    requirementDisplay = "Enrolled in a Tamil Nadu government primary school",
                    explanationNote = "Applicable to children studying Classes 1 to 5 in government schools.",
                    whyWeAskReason = "Scheme is designed for primary school children's nutrition support."
                ),
                EligibilityCriterion(
                    id = "tnbreakfast_state",
                    title = "State of Residence",
                    conditionType = CriterionConditionType.STATE_MATCH,
                    targetValue = "Tamil Nadu",
                    requirementDisplay = "Resident of Tamil Nadu",
                    explanationNote = "Restricted to Tamil Nadu government school students.",
                    whyWeAskReason = "State-funded scheme limited to Tamil Nadu government schools."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_bonafide", name = "Student ID / Bonafide Certificate", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Govt of Tamil Nadu School Education Dept (Prototype reference)",
            sourceUrl = "https://www.tnschools.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_one_student_one_laptop",
            name = "Tamil Nadu Free Laptop Scheme for Higher Secondary & College Students",
            shortName = "TN Free Laptop Scheme",
            tamilName = "தமிழ்நாடு இலவச மடிக்கணினி வழங்கும் திட்டம்",
            hindiName = "तमिलनाडु मुफ्त लैपटॉप योजना",
            category = SchemeCategory.EDUCATION,
            department = "School Education Department, Govt of Tamil Nadu",
            description = "Provides free laptops to meritorious students transitioning from Class 12 to undergraduate studies to bridge the digital learning divide.",
            benefitHighlight = "Free laptop for eligible transitioning students",
            detailedBenefits = listOf(
                "Distributed at government-organized district ceremonies",
                "Preloaded with e-learning content and educational software",
                "Improves access to online courses and digital exam preparation"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "laptop_student",
                    title = "Student Status",
                    conditionType = CriterionConditionType.STUDENT_STATUS,
                    targetValue = true,
                    requirementDisplay = "Completed Class 12 in a Tamil Nadu government school and enrolled in higher education",
                    explanationNote = "Priority given to top-performing students in government/aided schools.",
                    whyWeAskReason = "Scheme rewards academic merit at the school-to-college transition."
                ),
                EligibilityCriterion(
                    id = "laptop_state",
                    title = "State of Residence",
                    conditionType = CriterionConditionType.STATE_MATCH,
                    targetValue = "Tamil Nadu",
                    requirementDisplay = "Resident of Tamil Nadu",
                    explanationNote = "State-funded scheme restricted to Tamil Nadu students.",
                    whyWeAskReason = "Budget allocation is limited to Tamil Nadu residents."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_marksheet", name = "10th / 12th Marksheet", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_bonafide", name = "Student ID / Bonafide Certificate", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Govt of Tamil Nadu School Education Dept (Prototype reference)",
            sourceUrl = "https://www.tnschools.gov.in",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_disability_pension",
            name = "National Social Assistance Programme - Disability Pension (IGNDPS)",
            shortName = "IGNDPS Disability Pension",
            tamilName = "இந்திரா காந்தி ஊனமுற்றோர் ஓய்வூதியத் திட்டம்",
            hindiName = "इंदिरा गांधी राष्ट्रीय विकलांगता पेंशन योजना",
            category = SchemeCategory.WELFARE,
            department = "Ministry of Rural Development",
            description = "Provides a monthly pension to persons with severe or multiple disabilities living below the poverty line.",
            benefitHighlight = "Monthly pension of ₹300 to ₹500 (state top-ups may apply)",
            detailedBenefits = listOf(
                "Direct monthly pension credited to bank/post office account",
                "Combined eligibility with state disability welfare top-up schemes",
                "No contribution required from the beneficiary"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "igndps_disability",
                    title = "Disability Status",
                    conditionType = CriterionConditionType.DISABILITY_STATUS,
                    targetValue = "Yes",
                    requirementDisplay = "Severe or multiple disability (80% or above)",
                    explanationNote = "Requires valid UDID card or medical board certificate confirming severity.",
                    whyWeAskReason = "Statutory pension reserved for persons with severe disabilities."
                ),
                EligibilityCriterion(
                    id = "igndps_age",
                    title = "Age Range",
                    conditionType = CriterionConditionType.MIN_AGE,
                    targetValue = 18,
                    requirementDisplay = "Age 18 to 79 years",
                    explanationNote = "Beneficiaries above 80 are shifted to the old-age pension scheme.",
                    whyWeAskReason = "Determines which national pension scheme category applies."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_udid", name = "UDID / Disability Medical Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_ration_card", name = "Ration Card", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Rural Development (Prototype reference)",
            sourceUrl = "https://nsap.nic.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_widow_pension",
            name = "National Social Assistance Programme - Widow Pension (IGNWPS)",
            shortName = "IGNWPS Widow Pension",
            tamilName = "இந்திரா காந்தி விதவை ஓய்வூதியத் திட்டம்",
            hindiName = "इंदिरा गांधी राष्ट्रीय विधवा पेंशन योजना",
            category = SchemeCategory.WELFARE,
            department = "Ministry of Rural Development",
            description = "Provides a monthly pension to widows from below-poverty-line households to ensure basic financial security.",
            benefitHighlight = "Monthly pension of ₹300 to ₹500 (state top-ups may apply)",
            detailedBenefits = listOf(
                "Direct monthly pension credited to bank/post office account",
                "Combined with state widow welfare top-up schemes",
                "No repayment or contribution required"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "ignwps_gender",
                    title = "Gender & Marital Status",
                    conditionType = CriterionConditionType.GENDER_MATCH,
                    targetValue = "Female",
                    requirementDisplay = "Widow aged 40 to 79 years",
                    explanationNote = "Applicant must be a widow within the specified age bracket for this scheme.",
                    whyWeAskReason = "Scheme is exclusively for widowed women in this age range."
                ),
                EligibilityCriterion(
                    id = "ignwps_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 100000L,
                    requirementDisplay = "Family income ≤ ₹1,00,000 / year (BPL households)",
                    explanationNote = "Restricted to households below the poverty line.",
                    whyWeAskReason = "Ensures pension support prioritizes the poorest widowed women."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_death_cert", name = "Spouse Death Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_ration_card", name = "Ration Card", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of Rural Development (Prototype reference)",
            sourceUrl = "https://nsap.nic.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_saksham_scholarship",
            name = "AICTE Saksham Scholarship Scheme for Differently-Abled Students",
            shortName = "AICTE Saksham Scholarship",
            tamilName = "AICTE சக்ஷம் மாற்றுத்திறனாளிகள் உதவித்தொகைத் திட்டம்",
            hindiName = "एआईसीटीई सक्षम छात्रवृत्ति योजना",
            category = SchemeCategory.EDUCATION,
            department = "All India Council for Technical Education (AICTE), Ministry of Education",
            description = "Provides financial assistance to differently-abled students pursuing technical degree or diploma education to reduce economic burden and dropout.",
            benefitHighlight = "Up to ₹50,000 per year for tuition fee and incidentals",
            detailedBenefits = listOf(
                "Covers tuition and other fee components for AICTE-approved institutions",
                "Renewable annually based on satisfactory academic progress",
                "No repayment obligation — scholarship, not a loan"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "saksham_disability",
                    title = "Disability Status",
                    conditionType = CriterionConditionType.DISABILITY_STATUS,
                    targetValue = "Yes",
                    requirementDisplay = "Benchmark disability (40% or above)",
                    explanationNote = "Valid UDID card or medical certificate required for verification.",
                    whyWeAskReason = "Scholarship is reserved for differently-abled technical education students."
                ),
                EligibilityCriterion(
                    id = "saksham_student",
                    title = "Student Status",
                    conditionType = CriterionConditionType.STUDENT_STATUS,
                    targetValue = true,
                    requirementDisplay = "Enrolled in an AICTE-approved diploma or degree program",
                    explanationNote = "Applicable to first-year students in technical education programs.",
                    whyWeAskReason = "Ensures funds go to actively enrolled technical education students."
                ),
                EligibilityCriterion(
                    id = "saksham_income",
                    title = "Annual Family Income",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 800000L,
                    requirementDisplay = "Family income ≤ ₹8,00,000 / year",
                    explanationNote = "Broader income ceiling to accommodate the disability-focused welfare intent.",
                    whyWeAskReason = "Ensures the scholarship still targets non-affluent households."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_udid", name = "UDID / Disability Medical Certificate", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bonafide", name = "Student ID / Bonafide Certificate", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_income_cert", name = "Income Certificate", isMandatoryForEligibility = true, stage = "Document Verification")
            ),
            officialSourceLabel = "AICTE (Prototype reference)",
            sourceUrl = "https://www.aicte-india.org",
            lastVerifiedDate = "July 2026"
        ),

        Scheme(
            id = "sch_pm_surya_ghar",
            name = "PM Surya Ghar: Muft Bijli Yojana (Rooftop Solar Subsidy)",
            shortName = "PM Surya Ghar Solar Scheme",
            tamilName = "பிரதம மந்திரி சூர்யா கர் இலவச மின்சார திட்டம்",
            hindiName = "पीएम सूर्य घर मुफ्त बिजली योजना",
            category = SchemeCategory.HOUSING,
            department = "Ministry of New and Renewable Energy",
            description = "Provides subsidy for rooftop solar panel installation on residential homes to reduce electricity bills and promote clean energy adoption.",
            benefitHighlight = "Subsidy up to ₹78,000 for rooftop solar installation (2-3 kW systems)",
            detailedBenefits = listOf(
                "Central financial assistance credited directly to bank account post-installation",
                "Enables up to 300 units of free electricity per month for eligible households",
                "Low-interest collateral-free loan option for the remaining installation cost"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "surya_family",
                    title = "Owns Residential Property",
                    conditionType = CriterionConditionType.FAMILY_SIZE_MIN,
                    targetValue = 1,
                    requirementDisplay = "Owns a residential rooftop suitable for solar installation",
                    explanationNote = "Applicant must be the registered electricity connection holder for the residence.",
                    whyWeAskReason = "Subsidy requires proof of residential property/connection ownership."
                ),
                EligibilityCriterion(
                    id = "surya_income",
                    title = "Income Threshold",
                    conditionType = CriterionConditionType.MAX_INCOME,
                    targetValue = 1000000L,
                    requirementDisplay = "Family income ≤ ₹10,00,000 / year",
                    explanationNote = "Broad eligibility aimed at typical residential households.",
                    whyWeAskReason = "Ensures subsidy targets general residential adoption, not commercial-scale projects."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_electricity_bill", name = "Electricity Bill / Connection Proof", isMandatoryForEligibility = true, stage = "Document Verification"),
                SchemeDocument(id = "doc_bank_passbook", name = "Bank Account Passbook", isMandatoryForEligibility = true, stage = "Application Submission")
            ),
            officialSourceLabel = "Ministry of New and Renewable Energy (Prototype reference)",
            sourceUrl = "https://pmsuryaghar.gov.in",
            lastVerifiedDate = "August 2026"
        ),

        Scheme(
            id = "sch_svayam_scheme",
            name = "Deendayal Disabled Rehabilitation Scheme (DDRS)",
            shortName = "Deendayal Rehabilitation Scheme",
            tamilName = "தீன்தயாள் ஊனமுற்றோர் மீள்வாழ்வுத் திட்டம்",
            hindiName = "दीनदयाल विकलांग पुनर्वास योजना",
            category = SchemeCategory.WELFARE,
            department = "Department of Empowerment of Persons with Disabilities, MSJE",
            description = "Provides grants-in-aid to NGOs for rehabilitation services including special education, vocational training, and assistive devices for persons with disabilities.",
            benefitHighlight = "Free special education, vocational training, and assistive device support via empanelled NGOs",
            detailedBenefits = listOf(
                "Access to special schools and early intervention centers",
                "Vocational training linked to open and sheltered employment",
                "Distribution of aids and appliances at no cost through partner organizations"
            ),
            criteria = listOf(
                EligibilityCriterion(
                    id = "ddrs_disability",
                    title = "Disability Status",
                    conditionType = CriterionConditionType.DISABILITY_STATUS,
                    targetValue = "Yes",
                    requirementDisplay = "Any recognized benchmark disability",
                    explanationNote = "Valid UDID card or disability certificate required for enrollment at partner centers.",
                    whyWeAskReason = "Rehabilitation services are reserved for persons with disabilities."
                )
            ),
            requiredDocuments = listOf(
                SchemeDocument(id = "doc_aadhaar", name = "Aadhaar Card", isMandatoryForEligibility = true, stage = "Application Submission"),
                SchemeDocument(id = "doc_udid", name = "UDID / Disability Medical Certificate", isMandatoryForEligibility = true, stage = "Document Verification")
            ),
            officialSourceLabel = "Department of Empowerment of Persons with Disabilities (Prototype reference)",
            sourceUrl = "https://disabilityaffairs.gov.in",
            lastVerifiedDate = "July 2026"
        )
    )
}
