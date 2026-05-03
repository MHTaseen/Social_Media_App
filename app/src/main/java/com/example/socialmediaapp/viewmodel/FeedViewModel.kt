package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.model.Report
import com.example.socialmediaapp.model.ReportType
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.PostRepository
import com.example.socialmediaapp.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId = authRepository.currentUserId

    val currentUser: StateFlow<User?> = userRepository.getUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _locallyReportedIds = MutableStateFlow<Set<String>>(emptySet())

    val posts: StateFlow<List<Post>> = combine(
        postRepository.getPostsFlow(),
        currentUser,
        _locallyReportedIds
    ) { allPosts, user, locallyReported ->
        if (user == null) return@combine allPosts
        
        val reportedIds = user.reportedPostIds.toSet() + locallyReported
        
        allPosts.filter { post ->
            val isNotReported = !reportedIds.contains(post.id)
            val isOwnPost = post.userId == currentUserId
            val isFriend = user.friends.contains(post.userId)
            
            // Show if it's my post or a friend's post, AND it's not reported by me
            (isOwnPost || isFriend) && isNotReported
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _suggestedUsers = MutableStateFlow<List<User>>(emptyList())
    val suggestedUsers: StateFlow<List<User>> = _suggestedUsers

    init {
        loadSuggestions()
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val result = userRepository.getSuggestedUsers(currentUserId)
            _suggestedUsers.value = result.getOrDefault(emptyList())
        }
    }

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

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postRepository.deletePost(postId)
        }
    }

    fun reportPost(postId: String, reason: String = "Inappropriate content") {
        // Hide immediately in UI
        _locallyReportedIds.update { it + postId }
        
        viewModelScope.launch {
            val report = Report(
                reporterId = currentUserId,
                reportedItemId = postId,
                itemType = ReportType.POST,
                reason = reason
            )
            userRepository.reportPost(currentUserId, report)
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        viewModelScope.launch {
            userRepository.sendFriendRequest(currentUserId, targetUserId)
            loadSuggestions()
        }
    }
}