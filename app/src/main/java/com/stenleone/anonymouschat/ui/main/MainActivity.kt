package com.stenleone.anonymouschat.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stenleone.anonymouschat.navigation.LocalNavController
import com.stenleone.anonymouschat.navigation.Screen
import com.stenleone.anonymouschat.ui.chat.chatWidget
import com.stenleone.anonymouschat.ui.list.listWidget
import com.stenleone.anonymouschat.ui.theme.AnonymousChatTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AnonymousChatTheme {
                Surface(color = MaterialTheme.colors.background) {
                    val navController = rememberNavController()

                    CompositionLocalProvider(LocalNavController provides navController) {
                        NavHost(navController = navController, startDestination = Screen.ListWidget.name) {
                            composable(Screen.ListWidget.name) { listWidget() }
                            composable(
                                "${Screen.ChatWidget.name}/{${Screen.ChatWidget.chatId}}",
                                arguments = listOf(navArgument(Screen.ChatWidget.chatId) { type = NavType.StringType })
                            ) {
                                chatWidget(it.arguments?.getString(Screen.ChatWidget.chatId)!!)
                            }
                        }
                    }
                }
            }
        }

    }
}