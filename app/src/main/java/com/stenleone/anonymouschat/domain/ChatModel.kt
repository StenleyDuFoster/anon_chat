package com.stenleone.anonymouschat.domain

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatModel(
    val employee: ArrayList<String> = arrayListOf(),
    @Exclude
    @field:JvmField
    val lastMessage: ArrayList<MessageModel> = arrayListOf(),
    @DocumentId
    @field:JvmField
    val id: String = ""
) {

    fun getOpponentName(): String = kotlin.runCatching { employee.find { it != FirebaseAuth.getInstance().currentUser?.uid!! }!! }.getOrNull().orEmpty()

}