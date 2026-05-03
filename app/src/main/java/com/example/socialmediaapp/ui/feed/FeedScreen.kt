package com.example.socialmediaapp.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.socialmediaapp.model.MediaType
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.viewmodel.FeedViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(videoUrl)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(videoUrl) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val suggestions by viewModel.suggestedUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "SocialApp",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { navController.navigate(Screen.FriendRequests.route) }) {
                        val requestsCount = currentUser?.incomingRequests?.size ?: 0
                        BadgedBox(
                            badge = {
                                if (requestsCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) { 
                                        Text(requestsCount.toString(), color = Color.White) 
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.PeopleOutline, contentDescription = "Friend Requests")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreatePost.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Post", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (suggestions.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = "Suggested Friends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(suggestions) { user ->
                                SuggestedUserCard(
                                    user = user,
                                    onAddClick = { viewModel.sendFriendRequest(user.id) },
                                    onUserClick = { navController.navigate(Screen.Profile.route + "/${user.id}") }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp, 
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight(0.7f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No posts yet. Add friends to see their updates!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostItem(
                        post = post,
                        currentUserId = viewModel.currentUserId,
                        currentUser = currentUser,
                        onLikeClick = { viewModel.likePost(post.id) },
                        onUnlikeClick = { viewModel.unlikePost(post.id) },
                        onCommentClick = { 
                            navController.navigate(Screen.Comments.createRoute(post.id)) 
                        },
                        onUserClick = { navController.navigate(Screen.Profile.route + "/${post.userId}") },
                        onDeleteClick = { viewModel.deletePost(post.id) },
                        onAddFriendClick = { viewModel.sendFriendRequest(post.userId) },
                        onReportClick = { viewModel.reportPost(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedUserCard(
    user: User,
    onAddClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable(onClick = onUserClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profileImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = user.username,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAddClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Add", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    currentUserId: String,
    currentUser: User?,
    onLikeClick: () -> Unit,
    onUnlikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onUserClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onReportClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val timestamp = remember(post.timestamp) { timeFormat.format(post.timestamp.toDate()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.userProfileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(post.userProfileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onUserClick),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(onClick = onUserClick),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.username, 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable(onClick = onUserClick)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $timestamp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    
                    if (post.userId != currentUserId && currentUser != null) {
                        val isFriend = currentUser.friends.contains(post.userId)
                        val isSent = currentUser.outgoingRequests.contains(post.userId)
                        val isReceived = currentUser.incomingRequests.contains(post.userId)

                        if (!isFriend && !isSent && !isReceived) {
                            Text(
                                text = "Add Friend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onAddFriendClick)
                            )
                        } else if (isSent) {
                            Text(
                                text = "Request Sent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else if (post.userId == currentUserId) {
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (post.userId == currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Delete Post") },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Post") },
                                onClick = {
                                    showMenu = false
                                    onReportClick()
                                },
                                leadingIcon = { Icon(Icons.Default.ReportGmailerrorred, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            // Caption
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.2.sp
                    )
                )
            }

            // Media
            if (post.mediaUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .heightIn(min = 200.dp, max = 500.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val typeToShow = if (post.mediaType == MediaType.TEXT) {
                         if (post.mediaUrl.contains("video", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE
                    } else post.mediaType

                    if (typeToShow == MediaType.VIDEO) {
                        VideoPlayer(
                            videoUrl = post.mediaUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(post.mediaUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Post Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLiked = post.likes.contains(currentUserId)
                
                Surface(
                    onClick = { if (isLiked) onUnlikeClick() else onLikeClick() },
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.likes.size}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    onClick = onCommentClick,
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.commentCount}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = { /* Share functionality logic */ }) {
                    Icon(
                        Icons.Outlined.Share, 
                        contentDescription = "Share",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Feed.route,
            onClick = { 
                if (currentRoute != Screen.Feed.route) {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Feed.route) { inclusive = true }
                    }
                }
            },
            icon = { 
                Icon(
                    if (currentRoute == Screen.Feed.route) Icons.Default.DynamicFeed else Icons.Default.DynamicFeed, 
                    "Feed"
                ) 
            },
            label = { Text("Feed") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Search.route,
            onClick = { navController.navigate(Screen.Search.route) },
            icon = { Icon(Icons.Default.Search, "Search") },
            label = { Text("Search") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Messages.route,
            onClick = { navController.navigate(Screen.Messages.route) },
            icon = { 
                Icon(Icons.AutoMirrored.Filled.Chat, "Messages") 
            },
            label = { Text("Messages") },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            selected = currentRoute?.startsWith(Screen.Profile.route) == true,
            onClick = { navController.navigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("Profile") },
            alwaysShowLabel = false
        )
    }
}
