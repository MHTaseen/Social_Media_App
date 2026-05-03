package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {
    private val notificationsCollection = firestore.collection("notifications")

    override fun getNotificationsFlow(userId: String): Flow<List<Notification>> = callbackFlow {
        val listener = notificationsCollection
            .whereEqualTo("receiverId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Notification::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendNotification(notification: Notification): Result<Unit> = try {
        val docRef = notificationsCollection.document()
        notificationsCollection.document(docRef.id).set(notification.copy(id = docRef.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = try {
        notificationsCollection.document(notificationId).update("read", true).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun clearAll(userId: String): Result<Unit> = try {
        val snapshot = notificationsCollection.whereEqualTo("receiverId", userId).get().await()
        firestore.runBatch { batch ->
            snapshot.documents.forEach { batch.delete(it.reference) }
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
