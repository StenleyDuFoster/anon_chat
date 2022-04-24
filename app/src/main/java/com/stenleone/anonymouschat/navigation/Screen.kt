package com.stenleone.anonymouschat.navigation

sealed abstract class Screen(val name: String) {

    fun withParam(param: String) = "$name/$param"

    object ListWidget: Screen("listWidget")
    object ChatWidget: Screen("ChatWidget") {
        val chatId = "chatId"
    }
}