package com.stenleone.anonymouschat.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stenleone.anonymouschat.domain.ChatModel
import com.stenleone.anonymouschat.domain.Data
import com.stenleone.anonymouschat.domain.MessageModel
import com.stenleone.anonymouschat.domain.UserModel
import com.stenleone.anonymouschat.utill.exception.SomethinkWhentWrong
import com.stenleone.anonymouschat.utill.time.getNowUtc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirestoreManager {

    private val store = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun getChats() = channelFlow<Data<ArrayList<ChatModel>>> {
        Const.Chat().apply {
            val subscription = store.collection(value)
                .whereArrayContains(employee, FirebaseAuth.getInstance().currentUser?.uid.orEmpty())
                .addSnapshotListener { data, error ->
                    scope.launch {
                        if (error != null) {
                            this@channelFlow.send(Data.Error(error.message.orEmpty()))
                        }
                        runCatching {
                            this@channelFlow.send(Data.Success(data!!.toObjects(ChatModel::class.java).toCollection(ArrayList())))
                        }.onFailure {
                            this@channelFlow.send(Data.Error(it.message.orEmpty()))

                        }
                    }
                }
            awaitClose {
                subscription.remove()
            }
        }
    }

    suspend fun getUsers() = channelFlow<Data<ArrayList<UserModel>>> {
        Const.User().apply {
            val subscription = store.collection(value)
                .orderBy("lastAction")
                .addSnapshotListener { data, error ->
                    scope.launch {
                        if (error != null) {
                            this@channelFlow.send(Data.Error(error.message.orEmpty()))
                        }
                        runCatching {
                            this@channelFlow.send(Data.Success(data!!.toObjects(UserModel::class.java).toCollection(ArrayList()).apply {
                                removeIf { it.uid == FirebaseAuth.getInstance().currentUser?.uid!! }
                            }))
                        }.onFailure {
                            this@channelFlow.send(Data.Error(it.message.orEmpty()))

                        }
                    }
                }
            awaitClose {
                subscription.remove()
            }
        }
    }

    suspend fun getMessages(chatId: String) = channelFlow<Data<ArrayList<MessageModel>>> {
        Const.Chat.Communication().apply {
            val subscription = store.collection(value)
                .document(chatId)
                .collection(subValue)
                .orderBy("time")
                .addSnapshotListener { data, error ->
                    scope.launch {
                        if (error != null) {
                            this@channelFlow.send(Data.Error(error.message.orEmpty()))
                        }
                        runCatching {
                            this@channelFlow.send(Data.Success(data!!.toObjects(MessageModel::class.java).toCollection(ArrayList())))
                        }.onFailure {
                            this@channelFlow.send(Data.Error(it.message.orEmpty()))

                        }
                    }
                }
            awaitClose {
                subscription.remove()
            }
        }
    }

    suspend fun sendMessage(chatId: String, message: String) = suspendCoroutine<Unit> { emitter ->
        Const.Chat.Communication().apply {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                emitter.resumeWithException(NullPointerException())
                return@apply
            }
            store.collection(value)
                .document(chatId)
                .collection(subValue)
                .add(MessageModel(userId, value = message))
                .addOnCanceledListener {
                    emitter.resumeWithException(SomethinkWhentWrong())
                }
                .addOnSuccessListener {
                    emitter.resume(Unit)
                }
                .addOnFailureListener {
                    emitter.resumeWithException(it)
                }
        }
    }

    suspend fun saveUserToDb() = suspendCoroutine<Unit> { emitter ->
        Const.User().apply {
            FirebaseAuth.getInstance().currentUser?.let { user ->
                store.collection(value)
                    .document(user.uid)
                    .set(UserModel(user.uid, lastAction = Timestamp(getNowUtc())))
                    .addOnCanceledListener {
                        emitter.resumeWithException(SomethinkWhentWrong())
                    }
                    .addOnSuccessListener {
                        emitter.resume(Unit)
                    }
                    .addOnFailureListener {
                        emitter.resumeWithException(it)
                    }
            }
        }
    }

    suspend fun updateLastActionUser() = suspendCoroutine<Unit> { emitter ->
        Const.User().apply {
            FirebaseAuth.getInstance().currentUser?.let { user ->
                store.collection(value)
                    .document(user.uid)
                    .update(hashMapOf("lastAction" to Timestamp(getNowUtc())) as Map<String, Any>)
                    .addOnCanceledListener {
                        emitter.resumeWithException(SomethinkWhentWrong())
                    }
                    .addOnSuccessListener {
                        emitter.resume(Unit)
                    }
                    .addOnFailureListener {
                        emitter.resumeWithException(it)
                    }
            }
        }
    }

    suspend fun createChat(userId: String) = suspendCoroutine<Unit> { emitter ->
        Const.Chat().apply {
            val currentUserId = FirebaseAuth.getInstance().uid!!
            val listId = arrayListOf(currentUserId, userId).apply {
                var nonEqualIndex: Int
                var index = 0
                while (true) {
                    if (this.first()[index] != this.last()[index]) {
                        nonEqualIndex = index
                        break
                    }
                    index++
                }
                sortByDescending { it[nonEqualIndex] }
            }
            store.collection(value)
                .document("${listId.first()}+${listId.last()}")
                .set(
                    ChatModel(
                        employee = listId
                    )
                )
                .addOnCanceledListener {
                    emitter.resumeWithException(SomethinkWhentWrong())
                }
                .addOnSuccessListener {
                    emitter.resume(Unit)
                }
                .addOnFailureListener {
                    emitter.resumeWithException(it)
                }
        }
    }

    private sealed abstract class Const(val value: String) {

        class User : Const("user")
        open class Chat(value: String = "chat") : Const(value) {

            class Communication(val subValue: String = "communication") : Chat()

            val employee = "employee"

        }
    }

}