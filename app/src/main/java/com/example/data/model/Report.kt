package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int, // Foreign key (User id)
    val username: String, // Helper user name for direct display in general list
    val description: String,
    val photoPath: String, // URI or local asset path
    val latitude: Double,
    val longitude: Double,
    val locationDescription: String, // e.g. "Calle Morelos, Esquina Juárez"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Reportado", // "Reportado", "En Revisión", "Programado", "Reparado"
    val adminNote: String = "", // Admin response note
    val rating: Int = 1, // severity level: 1 = Low, 2 = Medium, 3 = High/Critical
    val upvotes: Int = 0 // Other citizens can upvote or confirm the same issue
)
