package com.stenleone.anonymouschat.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserModel(
    val uid: String? = null,
    val lastAction: Timestamp? = null
)