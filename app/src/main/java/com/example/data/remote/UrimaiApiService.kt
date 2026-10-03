package com.example.data.remote

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UrimaiApiService {

    @GET("schemes")
    suspend fun getSchemes(): Response<List<SchemeDto>>

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

    // --- Lawyer: own professional profile and verification documents ---

    @GET("lawyer/me")
    suspend fun getLawyerProfile(): Response<LawyerProfileResponse>

    @PUT("lawyer/me")
    suspend fun updateLawyerProfile(
        @Body request: UpdateLawyerProfileRequest
    ): Response<LawyerProfileResponse>

    @GET("lawyer/me/documents")
    suspend fun listVerificationDocuments(): Response<List<VerificationDocumentResponse>>

    @Multipart
    @POST("lawyer/me/documents")
    suspend fun uploadVerificationDocument(
        @Part("docType") docType: okhttp3.RequestBody,
        @Part file: MultipartBody.Part
    ): Response<VerificationDocumentResponse>

    @DELETE("lawyer/me/documents/{id}")
    suspend fun deleteVerificationDocument(@Path("id") id: String): Response<Unit>

    // --- Admin ---

    @GET("admin/users")
    suspend fun adminListUsers(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<AdminUserResponse>>

    @POST("admin/users/{id}/status")
    suspend fun adminSetAccountStatus(
        @Path("id") id: String,
        @Body request: AccountStatusRequest
    ): Response<AdminUserResponse>

    @GET("admin/lawyers")
    suspend fun adminListLawyers(
        @Query("status") status: String? = null
    ): Response<List<AdminLawyerResponse>>

    @GET("admin/lawyers/{id}/documents")
    suspend fun adminListLawyerDocuments(
        @Path("id") id: String
    ): Response<List<VerificationDocumentResponse>>

    @GET("admin/lawyers/{id}/documents/{docId}/file")
    suspend fun adminDownloadLawyerDocument(
        @Path("id") id: String,
        @Path("docId") docId: String
    ): Response<ResponseBody>

    @POST("admin/lawyers/{id}/verification")
    suspend fun adminDecideVerification(
        @Path("id") id: String,
        @Body request: VerificationDecisionRequest
    ): Response<Map<String, Any?>>

    @GET("admin/reports")
    suspend fun adminListReports(
        @Query("status") status: String? = null
    ): Response<List<ContentReportResponse>>

    @GET("admin/reports/{id}/content")
    suspend fun adminReportContent(
        @Path("id") id: String
    ): Response<ReportedContentResponse>

    @POST("admin/reports/{id}/resolve")
    suspend fun adminResolveReport(
        @Path("id") id: String,
        @Body request: ResolveReportRequest
    ): Response<Unit>

    @GET("admin/audit-log")
    suspend fun adminAuditLog(
        @Query("targetType") targetType: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<List<AuditLogEntryResponse>>

    // --- Notifications ---

    @GET("notifications")
    suspend fun listNotifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("limit") limit: Int = 50
    ): Response<List<NotificationResponse>>

    @GET("notifications/unread-count")
    suspend fun unreadNotificationCount(): Response<UnreadCountResponse>

    @POST("notifications/read")
    suspend fun markNotificationsRead(@Body request: MarkReadRequest): Response<Unit>

    // --- Citizen Q&A ---

    @POST("questions")
    suspend fun askQuestion(@Body request: AskQuestionRequest): Response<QuestionSummaryResponse>

    @GET("questions/mine")
    suspend fun myQuestions(): Response<List<QuestionSummaryResponse>>

    @GET("questions/{id}")
    suspend fun questionDetail(@Path("id") id: String): Response<QuestionDetailResponse>

    @POST("questions/{id}/close")
    suspend fun closeQuestion(@Path("id") id: String): Response<Unit>

    @POST("reports")
    suspend fun report(@Body request: ReportRequest): Response<Map<String, Any?>>

    // --- Lawyer Q&A ---

    @GET("lawyer/questions")
    suspend fun lawyerQuestionFeed(
        @Query("category") category: String? = null,
        @Query("state") state: String? = null,
        @Query("language") language: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<LawyerQuestionSummaryResponse>>

    @GET("lawyer/questions/{id}")
    suspend fun lawyerQuestionDetail(
        @Path("id") id: String
    ): Response<LawyerQuestionDetailResponse>

    @POST("lawyer/questions/{id}/answers")
    suspend fun submitAnswer(
        @Path("id") id: String,
        @Body request: SubmitAnswerRequest
    ): Response<MyAnswerBody>

    @PUT("lawyer/answers/{id}")
    suspend fun updateAnswer(
        @Path("id") id: String,
        @Body request: SubmitAnswerRequest
    ): Response<MyAnswerBody>

    @DELETE("lawyer/answers/{id}")
    suspend fun deleteAnswer(@Path("id") id: String): Response<Unit>

    @GET("lawyer/answers/mine")
    suspend fun myAnswers(): Response<List<MyAnswerSummaryResponse>>

    // --- Directory and proxied contact ---

    @GET("lawyers")
    suspend fun findLawyers(
        @Query("q") query: String? = null,
        @Query("state") state: String? = null,
        @Query("district") district: String? = null,
        @Query("specialization") specialization: String? = null,
        @Query("language") language: String? = null,
        @Query("minExperience") minExperience: Int? = null,
        @Query("availableOnly") availableOnly: Boolean? = null,
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int = 0
    ): Response<List<DirectoryLawyerResponse>>

    @GET("lawyers/{id}")
    suspend fun lawyerProfile(@Path("id") id: String): Response<DirectoryLawyerResponse>

    @POST("lawyers/{id}/contact-requests")
    suspend fun requestContact(
        @Path("id") id: String,
        @Body request: ContactRequestRequest
    ): Response<ContactRequestCreatedResponse>

    @GET("lawyers/contact-requests/mine")
    suspend fun myContactRequests(): Response<List<MyContactRequestResponse>>

    @GET("lawyers/me/inbox")
    suspend fun contactInbox(): Response<List<InboxRequestResponse>>

    @POST("lawyers/me/inbox/{id}/respond")
    suspend fun respondToContact(
        @Path("id") id: String,
        @Body request: RespondToContactRequest
    ): Response<Unit>

    @POST("lawyers/me/availability")
    suspend fun setAvailability(
        @Body request: AvailabilityRequest
    ): Response<AvailabilityResponse>

    // --- AI (server-side; the API key never reaches this client) ---

    @GET("ai/status")
    suspend fun aiStatus(): Response<AiStatusResponse>

    @POST("ai/explain-scheme")
    suspend fun explainScheme(
        @Body request: ExplainSchemeRequest
    ): Response<ExplainSchemeResponse>

    @POST("ai/scheme-chat")
    suspend fun schemeChat(
        @Body request: SchemeChatRequest
    ): Response<SchemeChatResponse>

    @Multipart
    @POST("ai/ocr/extract-profile")
    suspend fun extractProfileFromFile(
        @Part file: MultipartBody.Part
    ): Response<ExtractProfileResponse>

    @FormUrlEncoded
    @POST("ai/ocr/extract-profile")
    suspend fun extractProfileFromDocument(
        @Field("documentId") documentId: String
    ): Response<ExtractProfileResponse>
}
