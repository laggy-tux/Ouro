package com.example.red_panda.data.local.dao

import androidx.room.*
import com.example.red_panda.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE categoryId = :categoryId ORDER BY id ASC")
    fun getEventsForCategory(categoryId: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE categoryId = :categoryId ORDER BY id ASC")
    fun getEventsForCategorySync(categoryId: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventByIdSync(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: EventEntity): Long

    @Update
    fun updateEvent(event: EventEntity): Int

    @Delete
    fun deleteEvent(event: EventEntity): Int

    @Query("DELETE FROM events WHERE id = :id")
    fun deleteEventById(id: Long): Int

    @Query("SELECT * FROM events ORDER BY id ASC")
    fun getAllEventsSync(): List<EventEntity>

    @Query("SELECT COUNT(*) FROM events WHERE categoryId = :categoryId")
    fun getEventCountForCategory(categoryId: Long): Flow<Int>
}
