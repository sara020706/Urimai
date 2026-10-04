package com.example.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Obtains a Google ID token through Credential Manager.
 *
 * This class gets a token and nothing more. It never decides who the user is:
 * the token goes to the backend, which verifies Google's signature on it and
 * issues our own session. Reading the email out of the credential here and
 * trusting it would make the whole flow forgeable.
 *
 * Uses Credential Manager rather than the deprecated GoogleSignInClient API.
 */
object GoogleSignInClient {

    private const val TAG = "GoogleSignIn"

    /** Why a sign-in attempt did not produce a token. */
    sealed class Outcome {
        data class Success(val idToken: String) : Outcome()

        /** The person dismissed the sheet. Not an error; show nothing. */
        data object Cancelled : Outcome()

        /**
         * No Google account is available on the device, or none matched.
         * Distinguished from a hard failure so the message can be actionable.
         */
        data object NoAccount : Outcome()

        data class Failure(val message: String) : Outcome()
    }

    /** False when no client id was compiled in, so the UI can hide the button. */
    fun isConfigured(): Boolean = BuildConfig.GOOGLE_CLIENT_ID.isNotBlank()

    /**
     * @param activityContext must be an Activity context: Credential Manager
     *        shows a system sheet and cannot do that from an application context.
     * @param filterByAuthorized false so someone who has never used the app can
     *        still pick an account; true would show nothing on first run.
     */
    suspend fun getIdToken(
        activityContext: Context,
        filterByAuthorized: Boolean = false
    ): Outcome {
        if (!isConfigured()) {
            return Outcome.Failure("Google sign-in is not set up in this build.")
        }

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .setFilterByAuthorizedAccounts(filterByAuthorized)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val result = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                if (token.isBlank()) {
                    Outcome.Failure("Google did not return a sign-in token.")
                } else {
                    Outcome.Success(token)
                }
            } else {
                Outcome.Failure("Google returned an unexpected credential type.")
            }
        } catch (e: GetCredentialCancellationException) {
            Outcome.Cancelled
        } catch (e: NoCredentialException) {
            // Retry once without the authorized-accounts filter before giving
            // up: the common first-run case is simply that nothing matched yet.
            if (filterByAuthorized) {
                getIdToken(activityContext, filterByAuthorized = false)
            } else {
                Outcome.NoAccount
            }
        } catch (e: GetCredentialException) {
            Log.w(TAG, "credential request failed: ${e.javaClass.simpleName}")
            Outcome.Failure("Google sign-in could not be completed. Please try again.")
        } catch (e: Exception) {
            Log.w(TAG, "unexpected sign-in error: ${e.message}")
            Outcome.Failure("Google sign-in could not be completed. Please try again.")
        }
    }
}
