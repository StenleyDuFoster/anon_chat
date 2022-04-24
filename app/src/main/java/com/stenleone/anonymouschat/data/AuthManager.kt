package com.stenleone.anonymouschat.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

object AuthManager {

    private val auth = FirebaseAuth.getInstance()
    val signInState = MutableStateFlow(false)

    private var authScope: CoroutineScope? = null

    init {
        auth()
    }

    fun auth() {
        authScope?.cancel()
        authScope = CoroutineScope(Dispatchers.IO).apply {
            launch {
                if (auth.currentUser != null) {
                    FirestoreManager.saveUserToDb()
                    signInState.emit(true)
                } else {
                    auth.signInAnonymously()
                        .addOnSuccessListener {
                            launch {
                                FirestoreManager.saveUserToDb()
                                signInState.emit(true)
                            }
                        }
                        .addOnFailureListener {
                            launch {
                                signInState.emit(false)
                            }
                        }
                        .addOnCanceledListener {
                            launch {
                                signInState.emit(false)
                            }
                        }
                }
            }
        }
    }

}