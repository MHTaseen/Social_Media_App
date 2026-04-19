package com.example.socialmediaapp.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.socialmediaapp.ui.feed.BottomNavigationBar
import com.example.socialmediaapp.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val posts by viewModel.posts.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.username ?: "Profile") },
                actions = {
                    if (userId.isEmpty() || userId == viewModel.currentUserId) {
                        IconButton(onClick = { 
                            viewModel.logout()
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            user?.let { u ->
                ProfileHeader(
                    user = u,
                    currentUserId = viewModel.currentUserId,
                    onFollowClick = { viewModel.followUser() },
                    onUnfollowClick = { viewModel.unfollowUser() },
                    isFollowing = u.followers.contains(viewModel.currentUserId),
                    onFriendRequestClick = { viewModel.sendFriendRequest() },
                    onAcceptFriendRequest = { viewModel.acceptFriendRequest(it) },
                    onDeclineFriendRequest = { viewModel.declineFriendRequest(it) },
                    onCancelFriendRequest = { viewModel.cancelFriendRequest() },
                    onRemoveFriend = { viewModel.removeFriend() }
                )
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(posts) { post ->
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(1.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: com.example.socialmediaapp.model.User,
    currentUserId: String,
    isFollowing: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onFriendRequestClick: () -> Unit,
    onAcceptFriendRequest: (String) -> Unit,
    onDeclineFriendRequest: (String) -> Unit,
    onCancelFriendRequest: () -> Unit,
    onRemoveFriend: () -> Unit
) {
    val isCurrentUser = user.id == currentUserId
    val isFriend = user.friends.contains(currentUserId)
    val hasSentRequest = user.incomingRequests.contains(currentUserId)
    val hasIncomingRequest = user.outgoingRequests.contains(currentUserId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            contentScale = ContentScale.Crop
        )
        Text(text = user.fullName, style = MaterialTheme.typography.headlineMedium)
        Text(text = "@${user.username}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Text(text = user.email, style = MaterialTheme.typography.bodyMedium)
        if (user.bio.isNotEmpty()) {
            Text(text = user.bio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${user.followers.size}", style = MaterialTheme.typography.titleLarge)
                Text(text = "Followers")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${user.following.size}", style = MaterialTheme.typography.titleLarge)
                Text(text = "Following")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "${user.friends.size}", style = MaterialTheme.typography.titleLarge)
                Text(text = "Friends")
            }
        }
        
        if (!isCurrentUser) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (isFollowing) onUnfollowClick() else onFollowClick() }
                ) {
                    Text(if (isFollowing) "Unfollow" else "Follow")
                }

                when {
                    isFriend -> {
                        Button(onClick = onRemoveFriend, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Remove Friend")
                        }
                    }
                    hasSentRequest -> {
                        Button(onClick = onCancelFriendRequest) {
                            Text("Cancel Request")
                        }
                    }
                    hasIncomingRequest -> {
                        Column {
                            Button(onClick = { onAcceptFriendRequest(currentUserId) }) {
                                Text("Accept Friend")
                            }
                            TextButton(onClick = { onDeclineFriendRequest(currentUserId) }) {
                                Text("Decline")
                            }
                        }
                    }
                    else -> {
                        Button(onClick = onFriendRequestClick) {
                            Text("Add Friend")
                        }
                    }
                }
            }
        }
    }
}