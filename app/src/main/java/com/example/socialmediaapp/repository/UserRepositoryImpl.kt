package com.example.socialmediaapp.repository

import android.content.Context
import com.example.socialmediaapp.model.User
import com.example.socialmediaapp.model.Report
import com.example.socialmediaapp.model.Notification
import com.example.socialmediaapp.model.NotificationType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : UserRepository {
    private val usersCollection = firestore.collection("users")
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    // Trigger for mock mode updates
    private val mockUpdateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override suspend fun createUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        saveMockUser(user)
        Result.success(Unit)
    }

    private fun mergeUserWithMock(user: User?): User? {
        if (user == null) return null
        val mock = getMockUser(user.id) ?: return user
        return user.copy(
            friends = (user.friends + mock.friends).distinct(),
            incomingRequests = (user.incomingRequests + mock.incomingRequests).distinct(),
            outgoingRequests = (user.outgoingRequests + mock.outgoingRequests).distinct(),
            reportedPostIds = (user.reportedPostIds + mock.reportedPostIds).distinct(),
            profileImageUrl = if (user.profileImageUrl.isEmpty()) mock.profileImageUrl else user.profileImageUrl,
            bio = if (user.bio.isEmpty()) mock.bio else user.bio
        )
    }

    override suspend fun getUser(userId: String): Result<User?> = try {
        val snapshot = usersCollection.document(userId).get().await()
        val user = snapshot.toObject(User::class.java)
        Result.success(mergeUserWithMock(user ?: getMockUser(userId)))
    } catch (e: Exception) {
        Result.success(getMockUser(userId))
    }

    override fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            trySend(mergeUserWithMock(user ?: getMockUser(userId)))
        }
        
        val job = launch {
            mockUpdateSignal.collect {
                try {
                    val snapshot = usersCollection.document(userId).get().await()
                    val user = snapshot.toObject(User::class.java)
                    trySend(mergeUserWithMock(user ?: getMockUser(userId)))
                } catch (e: Exception) {
                    trySend(getMockUser(userId))
                }
            }
        }

        awaitClose { 
            listener.remove()
            job.cancel()
        }
    }.onStart { emit(getMockUser(userId)) }

    private fun getMockUser(userId: String): User? {
        val username = prefs.getString("user_name_$userId", null)
        val fullName = prefs.getString("user_full_name_$userId", null)
        val email = prefs.getString("user_email_$userId", null)
        
        if (username == null || email == null) return null
        
        return User(
            id = userId,
            username = username,
            fullName = fullName ?: "",
            email = email,
            profileImageUrl = prefs.getString("user_pic_$userId", "") ?: "",
            bio = prefs.getString("user_bio_$userId", "") ?: "",
            friends = getListFromPrefs("user_friends_$userId"),
            incomingRequests = getListFromPrefs("user_incoming_$userId"),
            outgoingRequests = getListFromPrefs("user_outgoing_$userId"),
            reportedPostIds = getListFromPrefs("user_reported_$userId")
        )
    }

    private fun saveMockUser(user: User) {
        prefs.edit().apply {
            putString("user_name_${user.id}", user.username)
            putString("user_full_name_${user.id}", user.fullName)
            putString("user_email_${user.id}", user.email)
            putString("user_pic_${user.id}", user.profileImageUrl)
            putString("user_bio_${user.id}", user.bio)
            putStringSet("user_friends_${user.id}", user.friends.toSet())
            putStringSet("user_incoming_${user.id}", user.incomingRequests.toSet())
            putStringSet("user_outgoing_${user.id}", user.outgoingRequests.toSet())
            putStringSet("user_reported_${user.id}", user.reportedPostIds.toSet())
            apply()
        }
        mockUpdateSignal.tryEmit(Unit)
    }

    private fun getListFromPrefs(key: String): List<String> {
        return prefs.getStringSet(key, emptySet())?.toList() ?: emptyList()
    }

    private fun updateMockList(userId: String, key: String, targetId: String, add: Boolean) {
        val currentSet = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (add) currentSet.add(targetId) else currentSet.remove(targetId)
        prefs.edit().putStringSet(key, currentSet).apply()
        mockUpdateSignal.tryEmit(Unit)
    }

    override suspend fun updateUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        saveMockUser(user)
        Result.success(Unit)
    }

    override suspend fun followUser(currentUserId: String, targetUserId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(currentUserId), "following", FieldValue.arrayUnion(targetUserId))
            batch.update(usersCollection.document(targetUserId), "followers", FieldValue.arrayUnion(currentUserId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(currentUserId), "following", FieldValue.arrayRemove(targetUserId))
            batch.update(usersCollection.document(targetUserId), "followers", FieldValue.arrayRemove(currentUserId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun searchUsers(query: String): Result<List<User>> = try {
        val snapshot = usersCollection
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThanOrEqualTo("username", query + "\uf8ff")
            .get().await()
        val users = snapshot.toObjects(User::class.java).toMutableList()
        
        val allPrefs = prefs.all
        allPrefs.keys.filter { it.startsWith("user_name_") }.forEach { key ->
            val userId = key.removePrefix("user_name_")
            val username = allPrefs[key] as? String ?: ""
            if (username.contains(query, ignoreCase = true) && users.none { it.id == userId }) {
                getMockUser(userId)?.let { users.add(it) }
            }
        }
        
        Result.success(users)
    } catch (e: Exception) {
        val users = mutableListOf<User>()
        val allPrefs = prefs.all
        allPrefs.keys.filter { it.startsWith("user_name_") }.forEach { key ->
            val userId = key.removePrefix("user_name_")
            val username = allPrefs[key] as? String ?: ""
            if (username.contains(query, ignoreCase = true)) {
                getMockUser(userId)?.let { users.add(it) }
            }
        }
        Result.success(users)
    }

    override suspend fun getSuggestedUsers(userId: String): Result<List<User>> = try {
        val currentUser = getUser(userId).getOrNull() ?: User(id = userId)
        
        val snapshot = usersCollection.limit(10).get().await()
        val users = snapshot.toObjects(User::class.java).toMutableList()
            .filter { it.id != userId && !currentUser.friends.contains(it.id) && !currentUser.outgoingRequests.contains(it.id) }
            .toMutableList()

        val allPrefs = prefs.all
        allPrefs.keys.filter { it.startsWith("user_name_") }.forEach { key ->
            val id = key.removePrefix("user_name_")
            if (id != userId && users.none { it.id == id } && 
                !currentUser.friends.contains(id) && !currentUser.outgoingRequests.contains(id)) {
                getMockUser(id)?.let { users.add(it) }
            }
        }
        
        Result.success(users.take(10))
    } catch (e: Exception) {
        val currentUser = getMockUser(userId) ?: User(id = userId)
        val users = mutableListOf<User>()
        val allPrefs = prefs.all
        allPrefs.keys.filter { it.startsWith("user_name_") }.forEach { key ->
            val id = key.removePrefix("user_name_")
            if (id != userId && !currentUser.friends.contains(id) && !currentUser.outgoingRequests.contains(id)) {
                getMockUser(id)?.let { users.add(it) }
            }
        }
        Result.success(users.take(10))
    }

    override suspend fun blockUser(currentUserId: String, targetUserId: String): Result<Unit> = try {
        usersCollection.document(currentUserId).update("blockedUsers", FieldValue.arrayUnion(targetUserId)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun sendFriendRequest(senderId: String, receiverId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(senderId), "outgoingRequests", FieldValue.arrayUnion(receiverId))
            batch.update(usersCollection.document(receiverId), "incomingRequests", FieldValue.arrayUnion(senderId))
        }.await()
        
        // Notification
        val sender = getUser(senderId).getOrNull()
        if (sender != null) {
            notificationRepository.sendNotification(
                Notification(
                    receiverId = receiverId,
                    senderId = senderId,
                    senderUsername = sender.username,
                    senderProfileImageUrl = sender.profileImageUrl,
                    type = NotificationType.FRIEND_REQUEST,
                    text = "sent you a friend request"
                )
            )
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        updateMockList(senderId, "user_outgoing_$senderId", receiverId, true)
        updateMockList(receiverId, "user_incoming_$receiverId", senderId, true)
        
        // Mock notification
        getMockUser(senderId)?.let { sender ->
            notificationRepository.sendNotification(
                Notification(
                    receiverId = receiverId,
                    senderId = senderId,
                    senderUsername = sender.username,
                    senderProfileImageUrl = sender.profileImageUrl,
                    type = NotificationType.FRIEND_REQUEST,
                    text = "sent you a friend request"
                )
            )
        }
        Result.success(Unit)
    }

    override suspend fun acceptFriendRequest(userId: String, requesterId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(userId), "incomingRequests", FieldValue.arrayRemove(requesterId))
            batch.update(usersCollection.document(userId), "friends", FieldValue.arrayUnion(requesterId))
            batch.update(usersCollection.document(requesterId), "outgoingRequests", FieldValue.arrayRemove(userId))
            batch.update(requesterId.let { firestore.collection("users").document(it) }, "friends", FieldValue.arrayUnion(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        updateMockList(userId, "user_incoming_$userId", requesterId, false)
        updateMockList(userId, "user_friends_$userId", requesterId, true)
        updateMockList(requesterId, "user_outgoing_$requesterId", userId, false)
        updateMockList(requesterId, "user_friends_$requesterId", userId, true)
        Result.success(Unit)
    }

    override suspend fun declineFriendRequest(userId: String, requesterId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(userId), "incomingRequests", FieldValue.arrayRemove(requesterId))
            batch.update(usersCollection.document(requesterId), "outgoingRequests", FieldValue.arrayRemove(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        updateMockList(userId, "user_incoming_$userId", requesterId, false)
        updateMockList(requesterId, "user_outgoing_$requesterId", userId, false)
        Result.success(Unit)
    }

    override suspend fun cancelFriendRequest(senderId: String, receiverId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(senderId), "outgoingRequests", FieldValue.arrayRemove(receiverId))
            batch.update(usersCollection.document(receiverId), "incomingRequests", FieldValue.arrayRemove(senderId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        updateMockList(senderId, "user_outgoing_$senderId", receiverId, false)
        updateMockList(receiverId, "user_incoming_$receiverId", senderId, false)
        Result.success(Unit)
    }

    override suspend fun removeFriend(userId: String, friendId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(userId), "friends", FieldValue.arrayRemove(friendId))
            batch.update(usersCollection.document(friendId), "friends", FieldValue.arrayRemove(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        updateMockList(userId, "user_friends_$userId", friendId, false)
        updateMockList(friendId, "user_friends_$friendId", userId, false)
        Result.success(Unit)
    }

    override suspend fun reportPost(userId: String, report: Report): Result<Unit> = try {
        firestore.runBatch { batch ->
            val reportRef = firestore.collection("reports").document()
            batch.set(reportRef, report.copy(id = reportRef.id))
            batch.update(usersCollection.document(userId), "reportedPostIds", FieldValue.arrayUnion(report.reportedItemId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        updateMockList(userId, "user_reported_$userId", report.reportedItemId, true)
        Result.success(Unit)
    }
}
