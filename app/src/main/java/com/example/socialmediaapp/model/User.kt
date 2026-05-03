package com.example.socialmediaapp.model

data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val friends: List<String> = emptyList(),
    val incomingRequests: List<String> = emptyList(),
    val outgoingRequests: List<String> = emptyList(),
    val reportedPostIds: List<String> = emptyList()
)