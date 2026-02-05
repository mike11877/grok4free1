package com.doomscrollpreventer.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_sessions",
    indices = [
        Index(value = ["packageName", "dateKey"], unique = true),
        Index(value = ["dateKey"])
    ]
)
data class UsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appDisplayName: String,
    val dateKey: String,           // "yyyy-MM-dd"
    val totalScrollTimeMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)
