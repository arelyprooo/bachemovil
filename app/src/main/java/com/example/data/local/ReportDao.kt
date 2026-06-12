package com.example.data.local

import androidx.room.*
import com.example.data.model.Report
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<Report>>

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getReportsByUser(userId: Int): Flow<List<Report>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report): Long

    @Update
    suspend fun updateReport(report: Report)

    @Query("SELECT * FROM reports WHERE id = :reportId LIMIT 1")
    fun getReportById(reportId: Int): Flow<Report?>

    @Delete
    suspend fun deleteReport(report: Report)

    @Query("UPDATE reports SET status = :status, adminNote = :adminNote WHERE id = :reportId")
    suspend fun updateReportStatus(reportId: Int, status: String, adminNote: String)

    @Query("UPDATE reports SET upvotes = upvotes + 1 WHERE id = :reportId")
    suspend fun upvoteReport(reportId: Int)
}
