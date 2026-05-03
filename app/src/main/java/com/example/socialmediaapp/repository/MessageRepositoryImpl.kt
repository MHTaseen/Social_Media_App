package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Chat
import com.example.socialmediaapp.model.Message
import com.example.socialmediaapp.model.Notification
import com.example.socialmediaapp.model.NotificationType
import com.example.socialmediaapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : MessageRepository {
    private val chatsCollection = firestore.collection("chats")

    override fun getChatsFlow(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCollection
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Chat::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override fun getMessagesFlow(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = chatsCollection.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(message: Message): Result<Unit> = try {
        val chatSnapshot = chatsCollection.document(message.chatId).get().await()
        val chat = chatSnapshot.toObject(Chat::class.java)
        
        firestore.runBatch { batch ->
            val chatRef = chatsCollection.document(message.chatId)
            val messageRef = chatRef.collection("messages").document()
            batch.set(messageRef, message.copy(id = messageRef.id))
            batch.update(chatRef, "lastMessage", message.text)
            batch.update(chatRef, "lastMessageTimestamp", message.timestamp)
        }.await()

        // Send notification to other participants
        chat?.participantIds?.filter { it != message.senderId }?.forEach { receiverId ->
            val sender = userRepository.getUser(message.senderId).getOrNull()
            if (sender != null) {
                notificationRepository.sendNotification(
                    Notification(
                        receiverId = receiverId,
                        senderId = message.senderId,
                        senderUsername = sender.username,
                        senderProfileImageUrl = sender.profileImageUrl,
                        type = NotificationType.MESSAGE,
                        text = message.text
                    )
                )
            }
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getOrCreateChat(participantIds: List<String>): Result<String> = try {
        val sortedIds = participantIds.sorted()
        val existingChat = chatsCollection
            .whereEqualTo("participantIds", sortedIds)
            .get().await()

        if (!existingChat.isEmpty) {
            Result.success(existingChat.documents[0].id)
        } else {
            val newChatRef = chatsCollection.document()
            val newChat = Chat(id = newChatRef.id, participantIds = sortedIds)
            newChatRef.set(newChat).await()
            Result.success(newChatRef.id)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
