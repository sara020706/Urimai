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
}
