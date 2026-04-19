package com.example.socialmediaapp.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.socialmediaapp.model.MediaType
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.ui.navigation.Screen
import com.example.socialmediaapp.viewmodel.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val posts by viewModel.posts.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("SocialApp") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreatePost.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Create Post")
            }
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
            items(posts, key = { it.id }) { post ->
                PostItem(
                    post = post,
                    currentUserId = viewModel.currentUserId,
                    onLikeClick = { viewModel.likePost(post.id) },
                    onUnlikeClick = { viewModel.unlikePost(post.id) },
                    onCommentClick = { 
                        navController.navigate(Screen.Comments.createRoute(post.id)) 
                    },
                    onUserClick = { navController.navigate(Screen.Profile.route + "/${post.userId}") }
                )
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    currentUserId: String,
    onLikeClick: () -> Unit,
    onUnlikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = post.userProfileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onUserClick) {
                    Text(text = post.username, style = MaterialTheme.typography.titleMedium)
                }
            }

            if (post.mediaUrl.isNotEmpty()) {
                AsyncImage(
                    model = post.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = post.content,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            Row(modifier = Modifier.padding(8.dp)) {
                val isLiked = post.likes.contains(currentUserId)
                IconButton(onClick = { if (isLiked) onUnlikeClick() else onLikeClick() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(text = "${post.likes.size}", modifier = Modifier.align(Alignment.CenterVertically))
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(onClick = onCommentClick) {
                    Icon(Icons.Default.Email, contentDescription = "Comment")
                }
                Text(text = "${post.commentCount}", modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        NavigationBarItem(
            selected = currentRoute == Screen.Feed.route,
            onClick = { navController.navigate(Screen.Feed.route) },
            icon = { Icon(Icons.Default.Email, "Feed") },
            label = { Text("Feed") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Search.route,
            onClick = { navController.navigate(Screen.Search.route) },
            icon = { Icon(Icons.Default.Search, "Search") },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Messages.route,
            onClick = { navController.navigate(Screen.Messages.route) },
            icon = { Icon(Icons.Default.Email, "Messages") },
            label = { Text("Messages") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { navController.navigate(Screen.Profile.route) },
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("Profile") }
        )
    }
}