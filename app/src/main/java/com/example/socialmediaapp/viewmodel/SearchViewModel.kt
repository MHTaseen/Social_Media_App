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
class SearchViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _suggestedUsers = MutableStateFlow<List<User>>(emptyList())
    val suggestedUsers: StateFlow<List<User>> = _suggestedUsers

    val currentUserId = authRepository.currentUserId
    
    val currentUser: StateFlow<User?> = userRepository.getUserFlow(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadSuggestions()
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            val result = userRepository.getSuggestedUsers(currentUserId)
            _suggestedUsers.value = result.getOrDefault(emptyList())
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            val result = userRepository.searchUsers(query)
            _searchResults.value = result.getOrDefault(emptyList())
        }
    }

    fun sendFriendRequest(targetUserId: String) {
        viewModelScope.launch {
            userRepository.sendFriendRequest(currentUserId, targetUserId)
            // Refresh suggestions
            loadSuggestions()
        }
    }
}