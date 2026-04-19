package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReportRepository {
    override suspend fun reportItem(report: Report): Result<Unit> = try {
        val docRef = firestore.collection("reports").document()
        firestore.collection("reports").document(docRef.id).set(report.copy(id = docRef.id)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}