package com.example.red_panda.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val date: String,
    val note: String = "",
    val location: String = "",
    val placeTag: String = "All Places",
    val personTag: String = "All People",
    val coverPhotoId: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val coverPhotoIsManual: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
