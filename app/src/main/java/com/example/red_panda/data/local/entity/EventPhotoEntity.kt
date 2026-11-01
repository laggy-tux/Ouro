package com.example.red_panda.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "event_photos",
    primaryKeys = ["eventId", "photoId"],
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhotoRefEntity::class,
            parentColumns = ["id"],
            childColumns = ["photoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId"), Index("photoId")]
)
data class EventPhotoEntity(
    val eventId: Long,
    val photoId: Long,
    @ColumnInfo(defaultValue = "0")
    val position: Long = 0
)
