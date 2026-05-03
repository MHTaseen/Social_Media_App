package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Post
import com.example.socialmediaapp.model.Comment
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun createPost(post: Post): Result<Unit>
    fun getPostsFlow(): Flow<List<Post>>
    fun getUserPostsFlow(userId: String): Flow<List<Post>>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun likePost(userId: String, postId: String): Result<Unit>
    suspend fun unlikePost(userId: String, postId: String): Result<Unit>
    
    suspend fun addComment(comment: Comment): Result<Unit>
    suspend fun deleteComment(commentId: String, postId: String): Result<Unit>
    fun getCommentsFlow(postId: String): Flow<List<Comment>>
}