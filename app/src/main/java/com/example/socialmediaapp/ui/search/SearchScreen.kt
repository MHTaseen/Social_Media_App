package com.example.socialmediaapp.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.ui.feed.BottomNavigationBar
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val suggestions by viewModel.suggestedUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchUsers(it)
                    },
                    placeholder = { Text("Search by username...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
            }
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            if (query.isBlank()) {
                if (suggestions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Suggestions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    items(suggestions) { user ->
                        SmartUserItem(
                            user = user,
                            currentUser = currentUser,
                            onUserClick = {
                                navController.navigate(Screen.Profile.route + "/${user.id}")
                            },
                            onAddClick = {
                                viewModel.sendFriendRequest(user.id)
                            }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Start typing to find friends",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                items(results) { user ->
                    SmartUserItem(
                        user = user,
                        currentUser = currentUser,
                        onUserClick = {
                            navController.navigate(Screen.Profile.route + "/${user.id}")
                        },
                        onAddClick = {
                            viewModel.sendFriendRequest(user.id)
                        }
                    )
                }
                
                if (results.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(), 
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No users found",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun SmartUserItem(
    user: User,
    currentUser: User?,
    onUserClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val isFriend = currentUser?.friends?.contains(user.id) ?: false
    val isSent = currentUser?.outgoingRequests?.contains(user.id) ?: false
    val isReceived = currentUser?.incomingRequests?.contains(user.id) ?: false
    val isMe = currentUser?.id == user.id

    Surface(
        onClick = onUserClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username, 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (user.bio.isNotEmpty()) {
                    Text(
                        text = user.bio, 
                        style = MaterialTheme.typography.bodySmall, 
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!isMe) {
                when {
                    isFriend -> {
                        AssistChip(
                            onClick = { /* Already friends */ },
                            label = { Text("Friends") },
                            leadingIcon = { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        )
                    }
                    isSent -> {
                        AssistChip(
                            onClick = { /* Already sent */ },
                            label = { Text("Sent") }
                        )
                    }
                    isReceived -> {
                        Button(
                            onClick = onAddClick,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Accept", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    else -> {
                        Button(
                            onClick = onAddClick,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.PersonAdd, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Add", 
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}