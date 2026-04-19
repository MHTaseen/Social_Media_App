package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Message
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.MessageRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    val currentUserId = authRepository.currentUserId

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            messageRepository.getMessagesFlow(chatId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val message = Message(
                chatId = chatId,
                senderId = currentUserId,
                text = text,
                timestamp = Timestamp.now()
            )
            messageRepository.sendMessage(message)
        }
    }
}