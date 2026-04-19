package com.example.socialmediaapp.model

import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String = "",
    val content: String = "",
    val mediaUrl: String = "",
    val mediaType: MediaType = MediaType.TEXT,
    val timestamp: Timestamp = Timestamp.now(),
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0
)

enum class MediaType {
    TEXT, IMAGE, VIDEO
}