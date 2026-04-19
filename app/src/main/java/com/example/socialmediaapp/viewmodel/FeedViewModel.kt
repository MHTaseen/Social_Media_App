package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val posts: StateFlow<List<Post>> = postRepository.getPostsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserId = authRepository.currentUserId

    fun likePost(postId: String) {
        viewModelScope.launch {
            postRepository.likePost(currentUserId, postId)
        }
    }

    fun unlikePost(postId: String) {
        viewModelScope.launch {
            postRepository.unlikePost(currentUserId, postId)
        }
    }
}