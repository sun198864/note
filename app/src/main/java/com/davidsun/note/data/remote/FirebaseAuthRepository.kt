package com.davidsun.note.data.remote

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.davidsun.note.domain.model.SignedInAccount
import com.davidsun.note.domain.repository.AuthRepository
import com.davidsun.note.domain.repository.CloudSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val cloudSync: CloudSyncCoordinator,
    @ApplicationContext context: Context,
) : AuthRepository {
    private val credentialManager = CredentialManager.create(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val account: StateFlow<SignedInAccount?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser.toAccount())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser.toAccount())

    override suspend fun signInWithGoogle(idToken: String): Boolean =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
        }.isSuccess

    override suspend fun signOut() {
        cloudSync.flush()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
        auth.signOut()
    }
}

private fun com.google.firebase.auth.FirebaseUser?.toAccount(): SignedInAccount? {
    val user = this ?: return null
    return SignedInAccount(
        uid = user.uid,
        email = user.email,
        displayName = user.displayName,
    )
}
