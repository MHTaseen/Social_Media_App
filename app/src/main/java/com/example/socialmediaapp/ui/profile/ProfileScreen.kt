package com.example.socialmediaapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.socialmediaapp.ui.feed.BottomNavigationBar
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileUser by viewModel.profileUser.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        profileUser?.username ?: "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
        ) {
            profileUser?.let { u ->
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
                    onRemoveFriend = { viewModel.removeFriend() },
                    onMessageClick = {
                        viewModel.getOrCreateChat { chatId ->
                            navController.navigate(Screen.Chat.createRoute(chatId))
                        }
                    }
                )

                val isFriend = u.friends.contains(viewModel.currentUserId)
                val isOwnProfile = u.id == viewModel.currentUserId

                if (isOwnProfile || isFriend) {
                    if (posts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 40.dp), 
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                "No posts yet", 
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            items(posts) { post ->
                                AsyncImage(
                                    model = post.mediaUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                } else {
                    // Hidden content for non-friends
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        color = Color.Transparent
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.padding(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "This account is private",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add this person as a friend to see their posts and updates.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
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
    onRemoveFriend: () -> Unit,
    onMessageClick: () -> Unit
) {
    val isCurrentUser = user.id == currentUserId
    val isFriend = user.friends.contains(currentUserId)
    val hasSentRequest = user.incomingRequests.contains(currentUserId)
    val hasIncomingRequest = user.outgoingRequests.contains(currentUserId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user.profileImageUrl.isNotEmpty()) {
            AsyncImage(
                model = user.profileImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.fullName, 
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
        )
        Text(
            text = "@${user.username}", 
            style = MaterialTheme.typography.bodyLarge, 
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        
        if (user.bio.isNotEmpty()) {
            Text(
                text = user.bio, 
                style = MaterialTheme.typography.bodyMedium, 
                modifier = Modifier.padding(top = 12.dp, start = 32.dp, end = 32.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat(label = "Followers", value = user.followers.size.toString())
            ProfileStat(label = "Following", value = user.following.size.toString())
            ProfileStat(label = "Friends", value = user.friends.size.toString())
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!isCurrentUser) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                if (isFriend) {
                    Button(
                        onClick = onMessageClick,
                        modifier = Modifier.weight(1f).height(45.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Message")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onRemoveFriend,
                        modifier = Modifier.weight(1f).height(45.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Unfriend")
                    }
                } else {
                    when {
                        hasSentRequest -> {
                            OutlinedButton(
                                onClick = onCancelFriendRequest,
                                modifier = Modifier.fillMaxWidth().height(45.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel Request")
                            }
                        }
                        hasIncomingRequest -> {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onAcceptFriendRequest(currentUserId) },
                                    modifier = Modifier.weight(1f).height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Accept")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = { onDeclineFriendRequest(currentUserId) },
                                    modifier = Modifier.weight(1f).height(45.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Decline")
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = onFriendRequestClick,
                                modifier = Modifier.fillMaxWidth().height(45.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Add Friend")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp
            )
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}