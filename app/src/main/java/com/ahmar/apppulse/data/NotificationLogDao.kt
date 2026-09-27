package com.ahmar.apppulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(log: NotificationLog)

    @Query("SELECT * FROM notification_log ORDER BY timestamp DESC LIMIT 50")
    fun getRecent(): Flow<List<NotificationLog>>

    @Query("DELETE FROM notification_log")
    suspend fun clearAll()
}
