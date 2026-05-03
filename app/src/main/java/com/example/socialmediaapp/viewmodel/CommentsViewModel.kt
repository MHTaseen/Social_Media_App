package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Comment
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.PostRepository
import com.example.socialmediaapp.repository.UserRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments

    val currentUserId = authRepository.currentUserId

    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getCommentsFlow(postId).collect {
                _comments.value = it
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val user = userRepository.getUser(currentUserId).getOrNull() ?: return@launch
            val comment = Comment(
                postId = postId,
                userId = currentUserId,
                username = user.username,
                userProfileImageUrl = user.profileImageUrl,
                text = text,
                timestamp = Timestamp.now()
            )
            postRepository.addComment(comment)
        }
    }

    fun deleteComment(commentId: String, postId: String) {
        viewModelScope.launch {
            postRepository.deleteComment(commentId, postId)
        }
    }
}