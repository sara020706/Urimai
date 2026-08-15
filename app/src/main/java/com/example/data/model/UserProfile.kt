package com.example.data.model

data class UserProfile(
    val name: String = "Citizen",
    val age: Int? = 21,
    val gender: String = "Male", // Male, Female, Other, Prefer not to say
    val state: String = "Tamil Nadu",
    val district: String = "Chennai",
    val occupation: String = "Student", // Student, Employed, Self-Employed, Unemployed, Homemaker, Farmer
    val education: String = "Undergraduate", // Below 10th, 10th Pass, 12th Pass, Diploma, Undergraduate, Postgraduate, Doctorate
    val annualIncome: Long? = 200000L, // in INR
    val familySize: Int = 4,
    val isStudent: Boolean? = true,
    val isEmployed: Boolean? = false,
    val isFarmer: Boolean? = false,
    val isBusinessOwner: Boolean? = false,
    val socialCategory: String? = "General / OBC", // General, OBC, SC, ST, EWS
    val disabilityStatus: String? = "No", // No, Yes (Locomotor), Yes (Visual), Yes (Hearing), Yes (Other)
    val maritalStatus: String? = "Single", // Single, Married, Widowed, Divorced/Separated
    val ownedDocuments: Set<String> = setOf(
        "Aadhaar Card",
        "Bank Account Passbook",
        "Student ID / Bonafide",
        "10th / 12th Marksheet"
    )
) {
    val isComplete: Boolean
        get() = age != null && state.isNotBlank() && annualIncome != null && isStudent != null && isEmployed != null

    fun getMissingRequiredFields(): List<String> {
        val missing = mutableListOf<String>()
        if (age == null) missing.add("Age")
        if (state.isBlank()) missing.add("State")
        if (annualIncome == null) missing.add("Annual Income")
        if (isStudent == null) missing.add("Student Status")
        if (isEmployed == null) missing.add("Employment Status")
        if (isFarmer == null) missing.add("Farmer Status")
        if (isBusinessOwner == null) missing.add("Business Ownership")
        return missing
    }

    companion object {
        val DEMO_ARUN_STUDENT = UserProfile(
            name = "Arun",
            age = 21,
            gender = "Male",
            state = "Tamil Nadu",
            district = "Chennai",
            occupation = "Student",
            education = "Undergraduate",
            annualIncome = 200000L,
            familySize = 4,
            isStudent = true,
            isEmployed = false,
            isFarmer = false,
            isBusinessOwner = false,
            socialCategory = "OBC",
            disabilityStatus = "No",
            maritalStatus = "Single",
            ownedDocuments = setOf("Aadhaar Card", "Bank Account Passbook", "Student ID / Bonafide", "10th / 12th Marksheet")
        )

        val DEMO_MEENA_FARMER = UserProfile(
            name = "Meena",
            age = 32,
            gender = "Female",
            state = "Tamil Nadu",
            district = "Madurai",
            occupation = "Farmer",
            education = "10th Pass",
            annualIncome = 180000L,
            familySize = 5,
            isStudent = false,
            isEmployed = true,
            isFarmer = true,
            isBusinessOwner = false,
            socialCategory = "MBC",
            disabilityStatus = "No",
            maritalStatus = "Married",
            ownedDocuments = setOf("Aadhaar Card", "Bank Account Passbook", "Land Ownership / Patta Document", "Income Certificate")
        )

        val DEMO_RAJESH_ENTREPRENEUR = UserProfile(
            name = "Rajesh",
            age = 29,
            gender = "Male",
            state = "Karnataka",
            district = "Bengaluru",
            occupation = "Self-Employed",
            education = "Diploma",
            annualIncome = 450000L,
            familySize = 3,
            isStudent = false,
            isEmployed = true,
            isFarmer = false,
            isBusinessOwner = true,
            socialCategory = "General",
            disabilityStatus = "No",
            maritalStatus = "Single",
            ownedDocuments = setOf("Aadhaar Card", "Bank Account Passbook", "PAN Card", "Business Registration / Udyam Certificate")
        )

        val DEMO_PRIYA_PARTIAL_DATA = UserProfile(
            name = "Priya",
            age = 23,
            gender = "Female",
            state = "Tamil Nadu",
            district = "Coimbatore",
            occupation = "Student",
            education = "Undergraduate",
            annualIncome = 220000L,
            familySize = 4,
            isStudent = true,
            isEmployed = null, // Missing field to trigger Scenario B
            isFarmer = false,
            isBusinessOwner = false,
            socialCategory = "SC",
            disabilityStatus = "No",
            maritalStatus = "Single",
            ownedDocuments = setOf("Aadhaar Card") // Missing income cert & marksheets to test document gap
        )
    }
}
