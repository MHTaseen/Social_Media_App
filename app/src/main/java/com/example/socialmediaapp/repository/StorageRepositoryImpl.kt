package com.example.socialmediaapp.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,
    @ApplicationContext private val context: Context
) : StorageRepository {
    override suspend fun uploadMedia(uri: Uri, path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ref = storage.reference.child(path)
            
            val mimeType = context.contentResolver.getType(uri)
            val isImage = mimeType?.startsWith("image") == true

            if (isImage) {
                val data = compressAndResizeImage(uri)
                if (data != null) {
                    ref.putBytes(data).await()
                } else {
                    ref.putFile(uri).await()
                }
            } else {
                ref.putFile(uri).await()
            }
            
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressAndResizeImage(uri: Uri): ByteArray? {
        return try {
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate inSampleSize to downsample the image
            // Targeted max resolution (e.g., 1080p)
            val reqWidth = 1080
            val reqHeight = 1080
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            // Decode bitmap with inSampleSize set
            val bitmap = context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val outputStream = ByteArrayOutputStream()
            // Compress to 70% quality JPEG
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val result = outputStream.toByteArray()
            bitmap.recycle()
            result
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}