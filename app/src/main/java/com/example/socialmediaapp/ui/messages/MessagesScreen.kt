package com.example.socialmediaapp.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.socialmediaapp.model.Chat
import com.example.socialmediaapp.ui.feed.BottomNavigationBar
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    navController: NavController,
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Messages") })
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(chats) { chat ->
                ChatItem(chat = chat, currentUserId = viewModel.currentUserId) {
                    navController.navigate(Screen.Chat.createRoute(chat.id))
                }
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, currentUserId: String, onClick: () -> Unit) {
    // In a real app, you'd fetch the other participant's user object here
    val otherParticipantId = chat.participantIds.find { it != currentUserId } ?: "Unknown"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for profile image
        Surface(
            modifier = Modifier.size(50.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = otherParticipantId.take(1).uppercase())
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(text = "User: $otherParticipantId", style = MaterialTheme.typography.titleMedium)
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}