package com.example.socialmediaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialmediaapp.model.Chat
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.repository.AuthRepository
import com.example.socialmediaapp.repository.MessageRepository
import com.example.socialmediaapp.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    data class ChatUiModel(
        val chat: Chat,
        val otherUser: User? = null
    )

    val currentUserId = authRepository.currentUserId

    @OptIn(ExperimentalCoroutinesApi::class)
    val chats: StateFlow<List<ChatUiModel>> = messageRepository.getChatsFlow(currentUserId)
        .flatMapLatest { chatList ->
            if (chatList.isEmpty()) return@flatMapLatest flowOf(emptyList<ChatUiModel>())
            
            val flows = chatList.map { chat ->
                val otherId = chat.participantIds.find { it != currentUserId } ?: ""
                userRepository.getUserFlow(otherId).map { user ->
                    ChatUiModel(chat, user)
                }
            }
            combine(flows) { it.toList() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
