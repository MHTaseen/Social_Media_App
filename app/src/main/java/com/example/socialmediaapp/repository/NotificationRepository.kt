package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotificationsFlow(userId: String): Flow<List<Notification>>
    suspend fun sendNotification(notification: Notification): Result<Unit>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun clearAll(userId: String): Result<Unit>
}
