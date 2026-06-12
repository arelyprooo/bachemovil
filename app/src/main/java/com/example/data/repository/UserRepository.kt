package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userDao: UserDao) {
    
    suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun registerUser(user: User): Result<Long> {
        val existingUser = userDao.getUserByUsername(user.username)
        if (existingUser != null) {
            return Result.failure(Exception("El nombre de usuario ya está registrado."))
        }
        val existingEmail = userDao.getUserByEmail(user.email)
        if (existingEmail != null) {
            return Result.failure(Exception("El correo electrónico ya está registrado."))
        }
        return try {
            val id = userDao.insertUser(user)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserById(userId: Int): Flow<User?> {
        return userDao.getUserById(userId)
    }
}
