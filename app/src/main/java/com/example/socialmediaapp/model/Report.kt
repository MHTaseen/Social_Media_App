package com.example.socialmediaapp.model

import com.google.firebase.Timestamp

data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedItemId: String = "",
    val itemType: ReportType = ReportType.POST,
    val reason: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

enum class ReportType {
    POST, USER, COMMENT
}