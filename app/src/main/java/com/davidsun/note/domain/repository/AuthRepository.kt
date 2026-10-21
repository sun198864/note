package com.davidsun.note.domain.repository

import com.davidsun.note.domain.model.SignedInAccount
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val account: StateFlow<SignedInAccount?>

    suspend fun signInWithGoogle(idToken: String): Boolean

    suspend fun signOut()
}
