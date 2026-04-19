package com.example.socialmediaapp.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage
) : StorageRepository {
    override suspend fun uploadImage(uri: Uri, path: String): Result<String> = try {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }
}