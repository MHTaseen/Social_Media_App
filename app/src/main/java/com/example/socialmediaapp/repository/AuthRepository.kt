package com.example.socialmediaapp.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String
    val authState: Flow<String?>
    
    suspend fun login(email: String, pass: String): Result<Unit>
    suspend fun register(email: String, pass: String, username: String, fullName: String): Result<Unit>
    suspend fun logout()
}