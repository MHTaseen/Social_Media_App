package com.example.socialmediaapp.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.searchUsers(it)
                        },
                        placeholder = { Text("Search users...") },
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        trailingIcon = { Icon(Icons.Default.Search, null) },
                        singleLine = true
                    )
                }
            )
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
            items(results) { user ->
                UserItem(user = user) {
                    navController.navigate(Screen.Profile.route + "/${user.id}")
                }
            }
        }
    }
}

@Composable
fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = null,
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = user.username, style = MaterialTheme.typography.titleMedium)
            if (user.bio.isNotEmpty()) {
                Text(text = user.bio, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}