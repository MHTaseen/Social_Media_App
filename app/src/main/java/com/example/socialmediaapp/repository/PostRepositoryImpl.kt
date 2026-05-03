package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Comment
import com.example.socialmediaapp.model.Notification
import com.example.socialmediaapp.model.NotificationType
import com.example.socialmediaapp.model.Post
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) : PostRepository {
    private val postsCollection = firestore.collection("posts")

    private val initialMockPosts = listOf(
        Post(
            id = "1",
            userId = "user1",
            username = "Taseen",
            content = "Welcome to my academic project social media app!",
            timestamp = Timestamp.now(),
            likes = listOf("user2"),
            commentCount = 2
        ),
        Post(
            id = "2",
            userId = "user2",
            username = "Android Dev",
            content = "Building apps with Jetpack Compose is amazing!",
            timestamp = Timestamp.now(),
            likes = emptyList(),
            commentCount = 5
        )
    )

    private val mockPosts = MutableStateFlow(initialMockPosts)

    override suspend fun createPost(post: Post): Result<Unit> = try {
        val docRef = postsCollection.document()
        val postId = docRef.id
        postsCollection.document(postId).set(post.copy(id = postId)).await()
        
        // Notify friends/followers about the new post
        val authorResult = userRepository.getUser(post.userId)
        val author = authorResult.getOrNull()
        if (author != null) {
            author.friends.forEach { friendId ->
                notificationRepository.sendNotification(
                    Notification(
                        receiverId = friendId,
                        senderId = post.userId,
                        senderUsername = author.username,
                        senderProfileImageUrl = author.profileImageUrl,
                        type = NotificationType.NEW_POST,
                        postId = postId,
                        text = "posted a new update"
                    )
                )
            }
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        val newPost = post.copy(id = "mock_${System.currentTimeMillis()}")
        mockPosts.update { currentList ->
            listOf(newPost) + currentList
        }
        Result.success(Unit)
    }

    override fun getPostsFlow(): Flow<List<Post>> {
        val firestoreFlow = callbackFlow {
            val listener = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                    trySend(posts)
                }
            awaitClose { listener.remove() }
        }.onStart { emit(emptyList()) }

        return combine(firestoreFlow, mockPosts) { firestorePosts, localMockPosts ->
            (firestorePosts + localMockPosts)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
        }
    }

    override fun getUserPostsFlow(userId: String): Flow<List<Post>> = getPostsFlow().map { posts ->
        posts.filter { it.userId == userId }
    }

    override suspend fun deletePost(postId: String): Result<Unit> = try {
        postsCollection.document(postId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        mockPosts.update { currentList ->
            currentList.filter { it.id != postId }
        }
        Result.success(Unit)
    }

    override suspend fun likePost(userId: String, postId: String): Result<Unit> = try {
        postsCollection.document(postId).update("likes", FieldValue.arrayUnion(userId)).await()
        
        // Send notification to post owner
        val postSnapshot = postsCollection.document(postId).get().await()
        val post = postSnapshot.toObject(Post::class.java)
        if (post != null && post.userId != userId) {
            val liker = userRepository.getUser(userId).getOrNull()
            if (liker != null) {
                notificationRepository.sendNotification(
                    Notification(
                        receiverId = post.userId,
                        senderId = userId,
                        senderUsername = liker.username,
                        senderProfileImageUrl = liker.profileImageUrl,
                        type = NotificationType.LIKE,
                        postId = postId,
                        text = "liked your post"
                    )
                )
            }
        }
        
        Result.success(Unit)
    } catch (e: Exception) {
        mockPosts.update { currentList ->
            currentList.map { 
                if (it.id == postId) {
                    if (it.likes.contains(userId)) it else it.copy(likes = it.likes + userId)
                } else it
            }
        }
        Result.success(Unit)
    }

    override suspend fun unlikePost(userId: String, postId: String): Result<Unit> = try {
        postsCollection.document(postId).update("likes", FieldValue.arrayRemove(userId)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        mockPosts.update { currentList ->
            currentList.map { 
                if (it.id == postId) {
                    it.copy(likes = it.likes - userId)
                } else it
            }
        }
        Result.success(Unit)
    }

    override suspend fun addComment(comment: Comment): Result<Unit> = try {
        firestore.runBatch { batch ->
            val commentRef = postsCollection.document(comment.postId).collection("comments").document()
            batch.set(commentRef, comment.copy(id = commentRef.id))
            batch.update(postsCollection.document(comment.postId), "commentCount", FieldValue.increment(1))
        }.await()

        // Send notification to post owner
        val postSnapshot = postsCollection.document(comment.postId).get().await()
        val post = postSnapshot.toObject(Post::class.java)
        if (post != null && post.userId != comment.userId) {
            val commenter = userRepository.getUser(comment.userId).getOrNull()
            if (commenter != null) {
                notificationRepository.sendNotification(
                    Notification(
                        receiverId = post.userId,
                        senderId = comment.userId,
                        senderUsername = commenter.username,
                        senderProfileImageUrl = commenter.profileImageUrl,
                        type = NotificationType.COMMENT,
                        postId = comment.postId,
                        text = "commented: ${comment.text}"
                    )
                )
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        mockPosts.update { currentList ->
            currentList.map { 
                if (it.id == comment.postId) {
                    it.copy(commentCount = it.commentCount + 1)
                } else it
            }
        }
        Result.success(Unit)
    }

    override suspend fun deleteComment(commentId: String, postId: String): Result<Unit> = try {
        firestore.runBatch { batch ->
            val commentRef = postsCollection.document(postId).collection("comments").document(commentId)
            batch.delete(commentRef)
            batch.update(postsCollection.document(postId), "commentCount", FieldValue.increment(-1))
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        mockPosts.update { currentList ->
            currentList.map { 
                if (it.id == postId) {
                    it.copy(commentCount = (it.commentCount - 1).coerceAtLeast(0))
                } else it
            }
        }
        Result.success(Unit)
    }

    override fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = postsCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Comment::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}
