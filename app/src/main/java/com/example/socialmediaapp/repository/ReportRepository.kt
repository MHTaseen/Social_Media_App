package com.example.socialmediaapp.repository

import com.example.socialmediaapp.model.Report

interface ReportRepository {
    suspend fun reportItem(report: Report): Result<Unit>
}