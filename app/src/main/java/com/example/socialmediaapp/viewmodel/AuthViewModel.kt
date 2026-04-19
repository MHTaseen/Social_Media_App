package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val currentUserId = repository.currentUserId

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.login(email, pass)
            _authState.value = if (result.isSuccess) AuthState.Authenticated else AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    fun register(email: String, pass: String, username: String, fullName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.register(email, pass, username, fullName)
            _authState.value = if (result.isSuccess) AuthState.Authenticated else AuthState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Idle
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}