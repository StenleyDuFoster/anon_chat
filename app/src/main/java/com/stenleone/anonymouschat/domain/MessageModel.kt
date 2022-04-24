package com.stenleone.anonymouschat.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.stenleone.anonymouschat.utill.time.getNowUtc

@IgnoreExtraProperties
data class MessageModel(
    val from: String = "",
    val time: Timestamp = Timestamp(getNowUtc()),
    val value: String = ""
)