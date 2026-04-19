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

    fun createPost(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            _state.value = CreatePostState.Loading
            val userId = authRepository.currentUserId
            if (userId.isEmpty()) return@launch
            val userResult = userRepository.getUser(userId)
            val user = userResult.getOrNull() ?: return@launch

            var imageUrl = ""
            if (imageUri != null) {
                val uploadResult = storageRepository.uploadImage(imageUri, "posts/${userId}_${System.currentTimeMillis()}")
                imageUrl = uploadResult.getOrNull() ?: ""
            }

            val post = Post(
                userId = userId,
                username = user.username,
                userProfileImageUrl = user.profileImageUrl,
                content = content,
                mediaUrl = imageUrl,
                mediaType = if (imageUrl.isNotEmpty()) MediaType.IMAGE else MediaType.TEXT,
                timestamp = Timestamp.now()
            )

            val result = postRepository.createPost(post)
            _state.value = if (result.isSuccess) CreatePostState.Success else CreatePostState.Error(result.exceptionOrNull()?.message ?: "Error creating post")
        }
    }
}

sealed class CreatePostState {
    object Idle : CreatePostState()
    object Loading : CreatePostState()
    object Success : CreatePostState()
    data class Error(val message: String) : CreatePostState()
}