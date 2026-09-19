package com.example.data.repository

import android.content.Context
import com.example.data.model.UserProfile
import com.example.data.remote.ApiClient
import com.example.data.remote.ProfileResponse
import com.example.data.remote.UpdateProfileRequest

class ProfileRepository(context: Context) {
    private val api = ApiClient.getService(context)

    suspend fun getProfile(): UserProfile? {
        val response = api.getProfile()
        return response.body()?.toUserProfile()
    }

    suspend fun updateProfile(profile: UserProfile): UserProfile? {
        val request = UpdateProfileRequest(
            name = profile.name,
            age = profile.age,
            gender = profile.gender,
            state = profile.state,
            district = profile.district,
            occupation = profile.occupation,
            education = profile.education,
            annualIncome = profile.annualIncome,
            familySize = profile.familySize,
            isStudent = profile.isStudent,
            isEmployed = profile.isEmployed,
            isFarmer = profile.isFarmer,
            isBusinessOwner = profile.isBusinessOwner,
            socialCategory = profile.socialCategory,
            disabilityStatus = profile.disabilityStatus,
            maritalStatus = profile.maritalStatus,
            ownedDocuments = profile.ownedDocuments.toList()
        )
        val response = api.updateProfile(request)
        return response.body()?.toUserProfile()
    }

    private fun ProfileResponse.toUserProfile() = UserProfile(
        name = name,
        age = age,
        gender = gender,
        state = state,
        district = district,
        occupation = occupation,
        education = education,
        annualIncome = annualIncome,
        familySize = familySize,
        isStudent = isStudent,
        isEmployed = isEmployed,
        isFarmer = isFarmer,
        isBusinessOwner = isBusinessOwner,
        socialCategory = socialCategory,
        disabilityStatus = disabilityStatus,
        maritalStatus = maritalStatus,
        ownedDocuments = ownedDocuments.toSet()
    )
}
