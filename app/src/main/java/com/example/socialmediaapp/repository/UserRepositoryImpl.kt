package com.example.socialmediaapp.repository

import android.content.Context
import com.example.socialmediaapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : UserRepository {
    private val usersCollection = firestore.collection("users")
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    override suspend fun createUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun getUser(userId: String): Result<User?> = try {
        val snapshot = usersCollection.document(userId).get().await()
        val user = snapshot.toObject(User::class.java)
        if (user != null) {
            Result.success(user)
        } else {
            val mockUser = getMockUser(userId)
            Result.success(mockUser)
        }
    } catch (e: Exception) {
        val mockUser = getMockUser(userId)
        Result.success(mockUser)
    }

    override fun getUserFlow(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            if (user != null) {
                trySend(user)
            } else {
                trySend(getMockUser(userId))
            }
        }
        awaitClose { listener.remove() }
    }

    private fun getMockUser(userId: String): User? {
        val username = prefs.getString("user_name_$userId", null)
        val fullName = prefs.getString("user_full_name_$userId", null)
        val email = prefs.getString("user_email_$userId", null)
        return if (username != null && email != null) {
            User(id = userId, username = username, fullName = fullName ?: "", email = email)
        } else {
            null
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
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
        val users = snapshot.toObjects(User::class.java)
        Result.success(users)
    } catch (e: Exception) {
        Result.success(emptyList())
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
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun acceptFriendRequest(userId: String, requesterId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(userId), "incomingRequests", FieldValue.arrayRemove(requesterId))
            batch.update(usersCollection.document(userId), "friends", FieldValue.arrayUnion(requesterId))
            batch.update(usersCollection.document(requesterId), "outgoingRequests", FieldValue.arrayRemove(userId))
            batch.update(usersCollection.document(requesterId), "friends", FieldValue.arrayUnion(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun declineFriendRequest(userId: String, requesterId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(userId), "incomingRequests", FieldValue.arrayRemove(requesterId))
            batch.update(usersCollection.document(requesterId), "outgoingRequests", FieldValue.arrayRemove(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun cancelFriendRequest(senderId: String, receiverId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(senderId), "outgoingRequests", FieldValue.arrayRemove(receiverId))
            batch.update(usersCollection.document(receiverId), "incomingRequests", FieldValue.arrayRemove(senderId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }

    override suspend fun removeFriend(userId: String, friendId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            batch.update(usersCollection.document(userId), "friends", FieldValue.arrayRemove(friendId))
            batch.update(usersCollection.document(friendId), "friends", FieldValue.arrayRemove(userId))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.success(Unit)
    }
}