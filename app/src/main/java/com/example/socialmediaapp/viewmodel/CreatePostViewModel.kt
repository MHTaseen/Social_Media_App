package com.example.socialmediaapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.MediaType
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.PostRepository
import com.example.socialmediaapp.repository.StorageRepository
import com.example.socialmediaapp.repository.UserRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val storageRepository: StorageRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow<CreatePostState>(CreatePostState.Idle)
    val state: StateFlow<CreatePostState> = _state

    fun createPost(content: String, mediaUri: Uri?, mediaType: MediaType) {
        viewModelScope.launch {
            _state.value = CreatePostState.Loading
            try {
                val userId = authRepository.currentUserId
                if (userId.isEmpty()) {
                    _state.value = CreatePostState.Error("User not logged in")
                    return@launch
                }

                val userResult = userRepository.getUser(userId)
                val user = userResult.getOrNull()
                if (user == null) {
                    _state.value = CreatePostState.Error("User profile not found")
                    return@launch
                }

                var mediaUrl = ""
                if (mediaUri != null) {
                    val folder = if (mediaType == MediaType.VIDEO) "videos" else "posts"
                    val uploadResult = storageRepository.uploadMedia(mediaUri, "${folder}/${userId}_${System.currentTimeMillis()}")
                    
                    // If upload fails (e.g. no Firebase config), use local URI for mock display
                    mediaUrl = uploadResult.getOrNull() ?: mediaUri.toString()
                }

                val post = Post(
                    userId = userId,
                    username = user.username,
                    userProfileImageUrl = user.profileImageUrl,
                    content = content,
                    mediaUrl = mediaUrl,
                    mediaType = if (mediaUrl.isNotEmpty()) mediaType else MediaType.TEXT,
                    timestamp = Timestamp.now()
                )

                val result = postRepository.createPost(post)
                if (result.isSuccess) {
                    _state.value = CreatePostState.Success
                } else {
                    _state.value = CreatePostState.Error(result.exceptionOrNull()?.message ?: "Error creating post")
                }
            } catch (e: Exception) {
                _state.value = CreatePostState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
}

sealed class CreatePostState {
    object Idle : CreatePostState()
    object Loading : CreatePostState()
    object Success : CreatePostState()
    data class Error(val message: String) : CreatePostState()
}