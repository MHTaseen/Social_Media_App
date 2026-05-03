package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.MessageRepository
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
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _profileUser = MutableStateFlow<User?>(null)
    val profileUser: StateFlow<User?> = _profileUser

    val currentUserId = authRepository.currentUserId
    
    val currentUser: StateFlow<User?> = userRepository.getUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _allProfilePosts = MutableStateFlow<List<Post>>(emptyList())
    
    val posts: StateFlow<List<Post>> = combine(_allProfilePosts, currentUser, _profileUser) { posts, currUser, profUser ->
        if (currUser == null || profUser == null) return@combine emptyList()
        
        val isFriend = profUser.friends.contains(currentUserId)
        val isOwnProfile = profUser.id == currentUserId
        
        if (isOwnProfile || isFriend) {
            posts.filter { !currUser.reportedPostIds.contains(it.id) }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadUser(userId: String) {
        val idToLoad = if (userId.isNullOrEmpty()) currentUserId else userId
        
        viewModelScope.launch {
            userRepository.getUserFlow(idToLoad).collect {
                _profileUser.value = it
            }
        }
        viewModelScope.launch {
            postRepository.getUserPostsFlow(idToLoad).collect {
                _allProfilePosts.value = it
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun followUser() {
        val targetUserId = _profileUser.value?.id ?: return
        viewModelScope.launch {
            userRepository.followUser(currentUserId, targetUserId)
        }
    }

    fun unfollowUser() {
        val targetUserId = _profileUser.value?.id ?: return
        viewModelScope.launch {
            userRepository.unfollowUser(currentUserId, targetUserId)
        }
    }

    fun sendFriendRequest() {
        val targetUserId = _profileUser.value?.id ?: return
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
        val targetUserId = _profileUser.value?.id ?: return
        viewModelScope.launch {
            userRepository.cancelFriendRequest(currentUserId, targetUserId)
        }
    }

    fun removeFriend() {
        val targetUserId = _profileUser.value?.id ?: return
        viewModelScope.launch {
            userRepository.removeFriend(currentUserId, targetUserId)
        }
    }

    fun getOrCreateChat(onSuccess: (String) -> Unit) {
        val targetUserId = _profileUser.value?.id ?: return
        viewModelScope.launch {
            messageRepository.getOrCreateChat(listOf(currentUserId, targetUserId)).onSuccess {
                onSuccess(it)
            }
        }
    }
}