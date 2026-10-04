package com.example.data.remote

import android.content.Context
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// Set per build type in app/build.gradle.kts, not here.
//
// The deployed backend is https://backend-delta-red-60.vercel.app/ and is the
// default for both build types. Override with URIMAI_DEV_API_BASE_URL (debug,
// e.g. the emulator alias http://10.0.2.2:4000/ or a LAN address) or
// URIMAI_API_BASE_URL (release, which rejects anything but https://).
val BASE_URL: String = BuildConfig.API_BASE_URL.ifEmpty {
    // An unconfigured release build would otherwise fail later as a confusing
    // "connection refused" against the empty string. Say what is actually wrong.
    error(
        "No API base URL is configured. Build with " +
            "-PURIMAI_API_BASE_URL=https://your-backend.example.com/"
    )
}

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

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .apply {
                // Release builds log nothing. Even at BASIC the interceptor
                // writes every request line to logcat, which on a shared or
                // rooted device is a readable trace of what a citizen looked
                // up -- scheme ids, question ids, lawyer ids.
                if (BuildConfig.HTTP_LOGGING) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
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
