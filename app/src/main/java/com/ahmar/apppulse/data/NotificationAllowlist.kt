package com.ahmar.apppulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_allowlist")
data class NotificationAllowlist(
    @PrimaryKey
    val packageName: String,
    val enabled: Boolean
)
