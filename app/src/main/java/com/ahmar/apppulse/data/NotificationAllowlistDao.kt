package com.ahmar.apppulse.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationAllowlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: NotificationAllowlist)

    @Query("SELECT * FROM notification_allowlist")
    fun getAll(): Flow<List<NotificationAllowlist>>

    @Query("SELECT enabled FROM notification_allowlist WHERE packageName = :packageName LIMIT 1")
    suspend fun isEnabled(packageName: String): Boolean?

    @Query("SELECT * FROM notification_allowlist WHERE enabled = 1")
    suspend fun getEnabledPackages(): List<NotificationAllowlist>
}
