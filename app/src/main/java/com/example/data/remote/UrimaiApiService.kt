package com.example.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface UrimaiApiService {

    @POST("auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun logIn(@Body request: LogInRequest): Response<AuthResponse>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @PUT("profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ProfileResponse>

    @GET("documents")
    suspend fun listDocuments(): Response<List<DocumentResponse>>

    @Multipart
    @POST("documents")
    suspend fun uploadDocument(
        @Part("documentName") documentName: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<DocumentResponse>

    @DELETE("documents/{id}")
    suspend fun deleteDocument(@Path("id") id: String): Response<Unit>

    @GET("documents/{id}/file")
    suspend fun downloadDocument(@Path("id") id: String): Response<ResponseBody>

    @GET("saved-schemes")
    suspend fun getSavedSchemes(): Response<List<String>>

    @PUT("saved-schemes/{schemeId}")
    suspend fun saveScheme(@Path("schemeId") schemeId: String): Response<Unit>

    @DELETE("saved-schemes/{schemeId}")
    suspend fun unsaveScheme(@Path("schemeId") schemeId: String): Response<Unit>
}
