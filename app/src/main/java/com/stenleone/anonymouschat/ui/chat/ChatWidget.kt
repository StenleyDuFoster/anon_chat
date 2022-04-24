package com.stenleone.anonymouschat.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.stenleone.anonymouschat.data.FirestoreManager
import com.stenleone.anonymouschat.domain.Data
import com.stenleone.anonymouschat.domain.MessageModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun chatWidget(chatId: String) {

    val scope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(arrayListOf<MessageModel>()) }

    var loadingState by remember { mutableStateOf(true) }

    var inputText by remember { mutableStateOf(TextFieldValue("")) }

    if (loadingState) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).padding(10.dp, 0.dp), reverseLayout = true) {
                items(messages.size) { position ->
                    if (messages[position].from == FirebaseAuth.getInstance().currentUser?.uid) {
                        Row(Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.weight(0.2f))
                            Text(text = messages[position].value, Modifier.weight(0.8f), textAlign = TextAlign.End)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth()) {
                            Text(text = messages[position].value, Modifier.weight(0.8f), textAlign = TextAlign.Start)
                            Spacer(modifier = Modifier.weight(0.2f))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                TextField(value = inputText, onValueChange = {
                    inputText = it
                }, modifier = Modifier.weight(1f))
                Text(text = "send", Modifier.clickable {
                    if (inputText.text.isNotEmpty() && inputText.text.isNotBlank()) {
                        kotlin.runCatching {
                            scope.launch {
                                FirestoreManager.sendMessage(chatId, inputText.text)
                                inputText = TextFieldValue("")
                            }
                        }
                    }
                })
            }
        }
    }

    LaunchedEffect(key1 = Unit, block = {

        FirestoreManager.getMessages(chatId).collect {
            if (loadingState) {
                loadingState = false
            }
            if (it is Data.Success) {
                messages = it.data.apply { reverse() }
            }
        }

    })

}