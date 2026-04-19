package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.PostRepository
import com.example.socialmediaapp.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    val currentUserId = authRepository.currentUserId

    fun loadUser(userId: String) {
        val idToLoad = if (userId.isNullOrEmpty()) currentUserId else userId
        
        viewModelScope.launch {
            userRepository.getUserFlow(idToLoad).collect {
                _user.value = it
            }
        }
        viewModelScope.launch {
            postRepository.getUserPostsFlow(idToLoad).collect {
                _posts.value = it
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun followUser() {
        val targetUserId = _user.value?.id ?: return
        viewModelScope.launch {
            userRepository.followUser(currentUserId, targetUserId)
        }
    }

    fun unfollowUser() {
        val targetUserId = _user.value?.id ?: return
        viewModelScope.launch {
            userRepository.unfollowUser(currentUserId, targetUserId)
        }
    }

    fun sendFriendRequest() {
        val targetUserId = _user.value?.id ?: return
        viewModelScope.launch {
            userRepository.sendFriendRequest(currentUserId, targetUserId)
        }
    }

    fun acceptFriendRequest(requesterId: String) {
        viewModelScope.launch {
            userRepository.acceptFriendRequest(currentUserId, requesterId)
        }
    }

    fun declineFriendRequest(requesterId: String) {
        viewModelScope.launch {
            userRepository.declineFriendRequest(currentUserId, requesterId)
        }
    }

    fun cancelFriendRequest() {
        val targetUserId = _user.value?.id ?: return
        viewModelScope.launch {
            userRepository.cancelFriendRequest(currentUserId, targetUserId)
        }
    }

    fun removeFriend() {
        val targetUserId = _user.value?.id ?: return
        viewModelScope.launch {
            userRepository.removeFriend(currentUserId, targetUserId)
        }
    }
}