package com.ahmar.apppulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_categories")
data class AppCategory(
    @PrimaryKey
    val packageName: String,
    val category: String
)
