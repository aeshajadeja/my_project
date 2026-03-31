package com.example.my_project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("SELECT * FROM chat_messages WHERE (senderId = :user1 AND receiverId = :user2) OR (senderId = :user2 AND receiverId = :user1) ORDER BY timestamp ASC")
    fun getChatHistory(user1: String, user2: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE receiverId = :currentUserId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastReceivedMessage(currentUserId: String): ChatMessage?
}
