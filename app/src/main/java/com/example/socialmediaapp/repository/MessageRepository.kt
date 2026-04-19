package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Chat
import com.example.socialmediaapp.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getChatsFlow(userId: String): Flow<List<Chat>>
    fun getMessagesFlow(chatId: String): Flow<List<Message>>
    suspend fun sendMessage(message: Message): Result<Unit>
    suspend fun getOrCreateChat(participantIds: List<String>): Result<String>
}