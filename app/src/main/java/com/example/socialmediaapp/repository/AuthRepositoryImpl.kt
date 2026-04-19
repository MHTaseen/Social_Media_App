package com.example.socialmediaapp.repository

import android.content.Context
import com.example.socialmediaapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : AuthRepository {
    
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private var mockUserId: String? = prefs.getString("current_mock_id", null)

    override val currentUserId: String
        get() = auth.currentUser?.uid ?: mockUserId ?: ""

    override val authState: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener {
            trySend(it.currentUser?.uid ?: mockUserId)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, pass: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, pass).await()
        Result.success(Unit)
    } catch (e: Exception) {
        val savedPass = prefs.getString("pwd_$email", null)
        val savedId = prefs.getString("id_$email", null)
        if (savedPass == pass && savedId != null) {
            mockUserId = savedId
            prefs.edit().putString("current_mock_id", savedId).apply()
            Result.success(Unit)
        } else {
            Result.failure(Exception("Invalid credentials or Firebase not configured."))
        }
    }

    override suspend fun register(email: String, pass: String, username: String, fullName: String): Result<Unit> = try {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        val userId = result.user?.uid ?: throw Exception("User ID is null")
        
        val user = User(id = userId, username = username, fullName = fullName, email = email)
        firestore.collection("users").document(userId).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        if (email.contains("@") && pass.length >= 6) {
            val newId = "mock_user_${System.currentTimeMillis()}"
            prefs.edit().apply {
                putString("pwd_$email", pass)
                putString("id_$email", newId)
                putString("user_name_$newId", username)
                putString("user_full_name_$newId", fullName)
                putString("user_email_$newId", email)
                putString("current_mock_id", newId)
                apply()
            }
            mockUserId = newId
            Result.success(Unit)
        } else {
            Result.failure(Exception("Registration failed: ${e.localizedMessage}"))
        }
    }

    override suspend fun logout() {
        auth.signOut()
        mockUserId = null
        prefs.edit().remove("current_mock_id").apply()
    }
}