package com.example.socialmediaapp.model

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfileImageUrl: String = "",
    val text: String = "",
    val timestamp: Timestamp = Timestamp.now()
)