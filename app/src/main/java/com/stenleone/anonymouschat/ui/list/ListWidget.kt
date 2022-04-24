package com.stenleone.anonymouschat.ui.list

import androidx.compose.animation.*
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.stenleone.anonymouschat.R
import com.stenleone.anonymouschat.data.AuthManager
import com.stenleone.anonymouschat.data.FirestoreManager
import com.stenleone.anonymouschat.domain.ChatModel
import com.stenleone.anonymouschat.domain.Data
import com.stenleone.anonymouschat.domain.UserModel
import com.stenleone.anonymouschat.navigation.LocalNavController
import com.stenleone.anonymouschat.navigation.Screen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalAnimationApi::class)
@Composable
fun listWidget() {

    var listState by remember {
        mutableStateOf<ListState>(ListState.Loading)
    }

    var chatModels by remember {
        mutableStateOf(arrayListOf<ChatModel>())
    }

    var userModels by remember {
        mutableStateOf(arrayListOf<UserModel>())
    }

    val snackbarScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    AnimatedContent(targetState = listState, transitionSpec = {
        if (targetState == ListState.ShowContent) {
            slideInVertically({ height -> height }) + fadeIn() with
                    slideOutVertically({ height -> -height }) + fadeOut()
        } else {
            slideInVertically({ height -> -height }) + fadeIn() with
                    slideOutVertically({ height -> height }) + fadeOut()
        }.using(
            SizeTransform(clip = false)
        )
    }) { state ->
        when (state) {
            ListState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            ListState.ShowContent -> {
                HorizontalPager(count = 2) { page ->
                    if (page == 0) {
                        val scrollState = rememberScrollState()
                        Column(Modifier.fillMaxSize()) {
                            Text(
                                stringResource(R.string.app_name),
                                Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp), textAlign = TextAlign.Center
                            )
                            if (chatModels.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(R.string.you_do_not_have_chat),
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp), textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                val navController = LocalNavController.current
                                LazyColumn(
                                    Modifier
                                        .fillMaxSize()
                                ) {
                                    items(chatModels.size) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .background(if (it % 2 == 0) Color.Gray else Color.White), horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(stringResource(id = R.string.chat_with, chatModels[it].getOpponentName()), Modifier.padding(10.dp).clickable {
                                                navController.navigate(Screen.ChatWidget.withParam(chatModels[it].id))
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    } else if (page == 1) {
                        val scrollState = rememberScrollState()
                        Column(
                            Modifier
                                .fillMaxSize()
                        ) {
                            Text(
                                stringResource(R.string.user_list),
                                Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp), textAlign = TextAlign.Center
                            )

                            var showCreateChatDialog by remember { mutableStateOf(false) }
                            var selectedUser by remember { mutableStateOf<UserModel?>(null) }

                            Box(Modifier.fillMaxSize()) {
                                if (showCreateChatDialog) {
                                    AlertDialog(onDismissRequest = { showCreateChatDialog = false }, buttons = {
                                        val scope = rememberCoroutineScope()
                                        var isLoading by remember { mutableStateOf(false) }

                                        AnimatedContent(targetState = isLoading, transitionSpec = {
                                            fadeIn(animationSpec = tween(150, 150)) with
                                                    fadeOut(animationSpec = tween(150)) using
                                                    SizeTransform { initialSize, targetSize ->
                                                        if (targetState) {
                                                            keyframes {
                                                                IntSize(targetSize.width, initialSize.height) at 150
                                                                durationMillis = 300
                                                            }
                                                        } else {
                                                            keyframes {
                                                                IntSize(initialSize.width, targetSize.height) at 150
                                                                durationMillis = 300
                                                            }
                                                        }
                                                    }
                                        }) { state ->
                                            if (state) {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(Modifier.padding(10.dp))
                                                }
                                            } else {
                                                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        stringResource(R.string.you_want_create_chat, selectedUser?.uid.orEmpty()),
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(10.dp), textAlign = TextAlign.Center
                                                    )

                                                    Button(onClick = {
                                                        scope.launch {
                                                            isLoading = true
                                                            kotlin.runCatching {
                                                                FirestoreManager.createChat(selectedUser?.uid!!)
                                                            }.onSuccess {
                                                                showCreateChatDialog = false
                                                            }.onFailure {
                                                                snackbarScope.launch {
                                                                    snackbarHostState.showSnackbar(it.message.orEmpty())
                                                                }
                                                                isLoading = false
                                                            }
                                                        }
                                                    }) {
                                                        Text(stringResource(id = R.string.create))
                                                    }
                                                }
                                            }
                                        }
                                    })
                                }

                                LazyColumn(Modifier.fillMaxSize()) {
                                    items(userModels.size) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .background(if (it % 2 == 0) Color.Gray else Color.White), horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(userModels.getOrNull(it)?.uid.orEmpty(), modifier = Modifier
                                                .padding(10.dp)
                                                .clickable {
                                                    selectedUser = userModels.getOrNull(it)
                                                    showCreateChatDialog = true
                                                })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        AuthManager.signInState.collect {
            if (it) {
                FirestoreManager.getChats().combine(FirestoreManager.getUsers()) { chats, users ->
                    Pair(chats, users)
                }.collect {
//                    if (listState is ListState.Loading) {
                        listState = ListState.ShowContent
//                    }
                    val chats = it.first
                    val users = it.second
                    if (chats is Data.Success<ArrayList<ChatModel>>) {
                        chatModels = chats.data
                    } else if (chats is Data.Error) {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar(chats.error)
                        }
                    }
                    if (users is Data.Success<ArrayList<UserModel>>) {
                        userModels = users.data
                    } else if (users is Data.Error) {
                        snackbarScope.launch {
                            snackbarHostState.showSnackbar(users.error)
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)

}

private sealed interface ListState {
    object Loading : ListState
    object ShowContent : ListState
}