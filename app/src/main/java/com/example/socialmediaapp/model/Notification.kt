package com.example.socialmediaapp.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val receiverId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderProfileImageUrl: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)

enum class NotificationType {
    LIKE, COMMENT, MESSAGE, NEW_POST, FRIEND_REQUEST
}
