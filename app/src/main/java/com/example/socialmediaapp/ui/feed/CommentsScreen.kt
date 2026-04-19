package com.example.socialmediaapp.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.socialmediaapp.model.Comment
import com.example.socialmediaapp.viewmodel.CommentsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    postId: String,
    navController: NavController,
    viewModel: CommentsViewModel = hiltViewModel()
) {
    val comments by viewModel.comments.collectAsState()
    var text by remember { mutableStateOf("") }

    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Comments") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(comments) { comment ->
                    CommentItem(comment = comment)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    viewModel.addComment(postId, text)
                    text = ""
                }) {
                    Text("Post")
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.userProfileImageUrl,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = comment.username, style = MaterialTheme.typography.titleSmall)
            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}