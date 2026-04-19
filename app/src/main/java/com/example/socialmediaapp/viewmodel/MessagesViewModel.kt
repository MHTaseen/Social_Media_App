package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Chat
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats

    val currentUserId = authRepository.currentUserId

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            messageRepository.getChatsFlow(currentUserId).collect {
                _chats.value = it
            }
        }
    }
}