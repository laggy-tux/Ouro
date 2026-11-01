package com.example.red_panda.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val coverPhotoId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val coverPhotoIsManual: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val section: String = "Other Albums",
    val coverColor: Long = 0xFF2C2C30
)
