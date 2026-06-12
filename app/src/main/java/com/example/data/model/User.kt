package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val email: String,
    val passwordHash: String, // simple hashed/hashed-text password
    val fullName: String,
    val dateRegistered: Long = System.currentTimeMillis()
)
