package com.davidsun.note.domain.model

data class SignedInAccount(
    val uid: String,
    val email: String?,
    val displayName: String?,
)
