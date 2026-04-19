package com.example.socialmediaapp.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Feed : Screen("feed")
    object CreatePost : Screen("create_post")
    object Profile : Screen("profile")
    object Search : Screen("search")
    object Messages : Screen("messages")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
}