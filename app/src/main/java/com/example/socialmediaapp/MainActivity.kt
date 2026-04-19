package com.example.socialmediaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.socialmediaapp.ui.auth.LoginScreen
import com.example.socialmediaapp.ui.auth.RegisterScreen
import com.example.socialmediaapp.ui.feed.CommentsScreen
import com.example.socialmediaapp.ui.feed.CreatePostScreen
import com.example.socialmediaapp.ui.feed.FeedScreen
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.ui.profile.ProfileScreen
import com.example.socialmediaapp.ui.search.SearchScreen
import com.example.socialmediaapp.ui.messages.MessagesScreen
import com.example.socialmediaapp.ui.messages.ChatScreen
import com.example.socialmediaapp.ui.theme.SocialMediaAppTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SocialMediaAppTheme {
                val navController = rememberNavController()
                val currentUser = FirebaseAuth.getInstance().currentUser
                val startDestination = if (currentUser != null) {
                    Screen.Feed.route
                } else {
                    Screen.Login.route
                }

                Scaffold { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(navController)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController)
                        }
                        composable(Screen.Feed.route) {
                            FeedScreen(navController)
                        }
                        composable(Screen.CreatePost.route) {
                            CreatePostScreen(navController)
                        }
                        composable(Screen.Search.route) {
                            SearchScreen(navController)
                        }
                        composable(Screen.Messages.route) {
                            MessagesScreen(navController)
                        }
                        composable(
                            route = Screen.Chat.route,
                            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
                            ChatScreen(chatId, navController)
                        }
                        composable(
                            route = Screen.Comments.route,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString("postId") ?: ""
                            CommentsScreen(postId, navController)
                        }
                        composable(
                            route = Screen.Profile.route + "/{userId}",
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val userId = backStackEntry.arguments?.getString("userId") ?: ""
                            ProfileScreen(userId, navController)
                        }
                        composable(Screen.Profile.route) {
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            ProfileScreen(currentUserId, navController)
                        }
                    }
                }
            }
        }
    }
}
