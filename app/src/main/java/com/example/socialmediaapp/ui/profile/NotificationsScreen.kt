package com.example.socialmediaapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.socialmediaapp.model.Notification
import com.example.socialmediaapp.model.NotificationType
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Notifications", 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.clearAll() }) {
                        Text("Clear All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            modifier = Modifier.padding(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No notifications yet", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(padding)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onNotificationClick = {
                            viewModel.markAsRead(notification.id)
                            when (notification.type) {
                                NotificationType.LIKE, NotificationType.COMMENT, NotificationType.NEW_POST -> {
                                    if (notification.postId.isNotEmpty()) {
                                        navController.navigate(Screen.Comments.createRoute(notification.postId))
                                    }
                                }
                                NotificationType.MESSAGE -> {
                                    navController.navigate(Screen.Messages.route)
                                }
                                NotificationType.FRIEND_REQUEST -> {
                                    navController.navigate(Screen.FriendRequests.route)
                                }
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val timestamp = timeFormat.format(notification.timestamp.toDate())

    Surface(
        onClick = onNotificationClick,
        color = if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                if (notification.senderProfileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = notification.senderProfileImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                
                val icon = when (notification.type) {
                    NotificationType.LIKE -> Icons.Default.Favorite
                    NotificationType.COMMENT -> Icons.AutoMirrored.Filled.Comment
                    NotificationType.MESSAGE -> Icons.AutoMirrored.Filled.Chat
                    NotificationType.NEW_POST -> Icons.Default.Add
                    NotificationType.FRIEND_REQUEST -> Icons.Default.PersonAdd
                }
                
                val iconColor = when (notification.type) {
                    NotificationType.LIKE -> Color.Red
                    NotificationType.COMMENT -> Color(0xFF2196F3)
                    NotificationType.MESSAGE -> Color(0xFF4CAF50)
                    NotificationType.NEW_POST -> Color(0xFFFF9800)
                    NotificationType.FRIEND_REQUEST -> Color(0xFF9C27B0)
                }

                Surface(
                    modifier = Modifier.size(20.dp),
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 2.dp
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(3.dp),
                        tint = iconColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(notification.senderUsername)
                        append(" ")
                        append(notification.text)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            if (!notification.isRead) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}
