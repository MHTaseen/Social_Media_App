package com.example.socialmediaapp.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class Chat(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now()
)