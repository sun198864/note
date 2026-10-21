package com.davidsun.note.feature.settings

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.davidsun.note.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleSignInAttempt {
    data class Token(val idToken: String) : GoogleSignInAttempt

    data object Cancelled : GoogleSignInAttempt

    data object Failed : GoogleSignInAttempt
}

class GoogleIdTokenRequester(
    private val context: Context,
) {
    suspend fun request(): GoogleSignInAttempt {
        val serverClientId = context.getString(R.string.default_web_client_id)
        if (serverClientId.isBlank()) return GoogleSignInAttempt.Failed
        val authorized = requestToken(serverClientId, filterByAuthorizedAccounts = true)
        if (authorized !is GoogleSignInAttempt.Failed) return authorized
        return requestToken(serverClientId, filterByAuthorizedAccounts = false)
    }

    private suspend fun requestToken(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
    ): GoogleSignInAttempt {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        return try {
            val result = CredentialManager.create(context).getCredential(context, request)
            val credential = result.credential
            if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleSignInAttempt.Failed
            } else {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                if (token.isBlank()) GoogleSignInAttempt.Failed else GoogleSignInAttempt.Token(token)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInAttempt.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInAttempt.Failed
        } catch (_: Exception) {
            GoogleSignInAttempt.Failed
        }
    }
}
