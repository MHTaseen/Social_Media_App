package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendRequestsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _friendRequests = MutableStateFlow<List<User>>(emptyList())
    val friendRequests: StateFlow<List<User>> = _friendRequests

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val currentUserId = authRepository.currentUserId

    init {
        loadFriendRequests()
    }

    private fun loadFriendRequests() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getUserFlow(currentUserId).collect { user ->
                val requests = user?.incomingRequests ?: emptyList()
                val requestUsers = mutableListOf<User>()
                requests.forEach { requesterId ->
                    userRepository.getUser(requesterId).getOrNull()?.let {
                        requestUsers.add(it)
                    }
                }
                _friendRequests.value = requestUsers
                _isLoading.value = false
            }
        }
    }

    fun acceptRequest(requesterId: String) {
        viewModelScope.launch {
            userRepository.acceptFriendRequest(currentUserId, requesterId)
        }
    }

    fun declineRequest(requesterId: String) {
        viewModelScope.launch {
            userRepository.declineFriendRequest(currentUserId, requesterId)
        }
    }
}