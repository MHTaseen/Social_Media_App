package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Comment
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
    private val firestore: FirebaseFirestore
) : PostRepository {
    private val postsCollection = firestore.collection("posts")

    private val mockPosts = MutableStateFlow(listOf(
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
    ))

    override suspend fun createPost(post: Post): Result<Unit> = try {
        val docRef = postsCollection.document()
        postsCollection.document(docRef.id).set(post.copy(id = docRef.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        val newPost = post.copy(id = "mock_${System.currentTimeMillis()}")
        mockPosts.update { currentList ->
            listOf(newPost) + currentList
        }
        Result.success(Unit)
    }

    override fun getPostsFlow(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                if (posts.isNotEmpty()) {
                    trySend(posts)
                }
            }
        
        val job = launch {
            mockPosts.collect { trySend(it) }
        }

        awaitClose { 
            listener.remove()
            job.cancel()
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

    override fun getCommentsFlow(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = postsCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObjects(Comment::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}