package com.example.socialmediaapp.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadMedia(uri: Uri, path: String): Result<String>
}