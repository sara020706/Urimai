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
        )
    )
}
