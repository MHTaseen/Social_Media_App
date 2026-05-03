package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.model.Report
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun createUser(user: User): Result<Unit>
    suspend fun getUser(userId: String): Result<User?>
    fun getUserFlow(userId: String): Flow<User?>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit>
    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun getSuggestedUsers(userId: String): Result<List<User>>
    suspend fun blockUser(currentUserId: String, targetUserId: String): Result<Unit>
    
    // Friend Request methods
    suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<Unit>
    suspend fun acceptFriendRequest(userId: String, requesterId: String): Result<Unit>
    suspend fun declineFriendRequest(userId: String, requesterId: String): Result<Unit>
    suspend fun cancelFriendRequest(senderId: String, receiverId: String): Result<Unit>
    suspend fun removeFriend(userId: String, friendId: String): Result<Unit>
    
    // Reporting
    suspend fun reportPost(userId: String, report: Report): Result<Unit>
}