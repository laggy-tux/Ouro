package com.example.red_panda.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_refs")
data class PhotoRefEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long? = null,
    val contentUri: String,
    val contentHash: String = "",
    val takenAt: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false,
    val placeholderColor: Long = 0xFF2C2C30
)
