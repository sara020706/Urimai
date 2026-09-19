package com.example.data.remote

import android.content.Context
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Prototype-only base URL. Point this at your deployed backend, or
// 10.0.2.2 (emulator loopback alias for the host machine) while running locally.
const val BASE_URL = "http://10.0.2.2:4000/"

object ApiClient {

    @Volatile private var service: UrimaiApiService? = null

    fun getService(context: Context): UrimaiApiService =
        service ?: synchronized(this) {
            service ?: build(context.applicationContext).also { service = it }
        }

    private fun build(context: Context): UrimaiApiService {
        val sessionManager = SessionManager(context)

        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = runBlocking { sessionManager.currentToken() }
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder().build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UrimaiApiService::class.java)
    }
}
