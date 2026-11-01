package com.example.red_panda.data.local.dao

import androidx.room.*
import com.example.red_panda.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("""
        SELECT DISTINCT categories.* FROM categories
        LEFT JOIN events ON events.categoryId = categories.id
        LEFT JOIN event_photos ON event_photos.eventId = events.id
        ORDER BY categories.isFavorite DESC, categories.createdAt DESC
    """)
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryByIdSync(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCategory(category: CategoryEntity): Long

    @Update
    fun updateCategory(category: CategoryEntity): Int

    @Delete
    fun deleteCategory(category: CategoryEntity): Int

    @Query("DELETE FROM categories WHERE id = :id")
    fun deleteCategoryById(id: Long): Int

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Int
}
