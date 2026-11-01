package com.example.red_panda.data.local.dao

import androidx.room.*
import com.example.red_panda.data.local.entity.EventPhotoEntity
import com.example.red_panda.data.local.entity.PhotoRefEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("""
        SELECT p.* FROM photo_refs p 
        INNER JOIN event_photos ep ON p.id = ep.photoId 
        WHERE ep.eventId = :eventId 
        ORDER BY ep.position ASC
    """)
    fun getPhotosForEvent(eventId: Long): Flow<List<PhotoRefEntity>>

    @Query("""
        SELECT p.* FROM photo_refs p 
        INNER JOIN event_photos ep ON p.id = ep.photoId 
        WHERE ep.eventId = :eventId 
        ORDER BY ep.position ASC
    """)
    fun getPhotosForEventSync(eventId: Long): List<PhotoRefEntity>

    @Query("""
        SELECT p.* FROM photo_refs p
        INNER JOIN event_photos ep ON p.id = ep.photoId
        INNER JOIN events e ON ep.eventId = e.id
        WHERE e.categoryId = :categoryId
        ORDER BY e.createdAt ASC, e.id ASC, ep.position ASC
        LIMIT 1
    """)
    fun getEarliestPhotoForCategorySync(categoryId: Long): PhotoRefEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM event_photos ep
            JOIN events e ON e.id = ep.eventId
            WHERE e.categoryId = :categoryId AND ep.photoId = :photoId
        )
    """)
    fun isPhotoInCategorySync(categoryId: Long, photoId: Long): Boolean

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM event_photos WHERE eventId = :eventId")
    fun getNextPositionForEventSync(eventId: Long): Long

    @Query("""
        SELECT DISTINCT p.* FROM photo_refs p
        INNER JOIN event_photos ep ON p.id = ep.photoId
        INNER JOIN events e ON ep.eventId = e.id
        WHERE e.categoryId = :categoryId
        ORDER BY p.takenAt DESC
    """)
    fun getPhotosForCategorySync(categoryId: Long): List<PhotoRefEntity>

    @Query("""
        SELECT DISTINCT p.* FROM photo_refs p
        INNER JOIN event_photos ep ON p.id = ep.photoId
        INNER JOIN events e ON ep.eventId = e.id
        WHERE e.categoryId = :categoryId
        ORDER BY p.takenAt DESC
    """)
    fun getPhotosForCategory(categoryId: Long): Flow<List<PhotoRefEntity>>

    @Query("""
        SELECT COUNT(DISTINCT ep.photoId) FROM event_photos ep
        INNER JOIN events e ON ep.eventId = e.id
        WHERE e.categoryId = :categoryId
    """)
    fun getPhotoCountForCategory(categoryId: Long): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT ep.photoId) FROM event_photos ep
        INNER JOIN events e ON ep.eventId = e.id
        WHERE e.categoryId = :categoryId
    """)
    fun getPhotoCountForCategorySync(categoryId: Long): Int

    @Query("SELECT contentUri FROM photo_refs")
    fun getAllTaggedPhotoUris(): List<String>

    @Query("SELECT DISTINCT contentUri FROM photo_refs")
    fun getAllTaggedPhotoUrisFlow(): Flow<List<String>>

    @Query("SELECT * FROM photo_refs WHERE contentUri = :contentUri LIMIT 1")
    fun getPhotoByUri(contentUri: String): PhotoRefEntity?

    @Query("SELECT * FROM photo_refs WHERE id = :photoId")
    fun getPhotoById(photoId: Long): Flow<PhotoRefEntity?>

    @Query("SELECT * FROM photo_refs WHERE id = :photoId LIMIT 1")
    fun getPhotoByIdSync(photoId: Long): PhotoRefEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPhoto(photo: PhotoRefEntity): Long

    @Update
    fun updatePhoto(photo: PhotoRefEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEventPhoto(eventPhoto: EventPhotoEntity): Long

    @Query("DELETE FROM event_photos WHERE eventId = :eventId AND photoId = :photoId")
    fun removePhotoFromEvent(eventId: Long, photoId: Long): Int

    @Query("SELECT * FROM photo_refs")
    fun getAllPhotosSync(): List<PhotoRefEntity>

    @Query("UPDATE photo_refs SET isFavorite = :isFavorite WHERE id = :photoId")
    fun updateFavorite(photoId: Long, isFavorite: Boolean)

    @Query("UPDATE photo_refs SET isFavorite = :isFavorite WHERE contentUri = :contentUri")
    fun updateFavoriteByUri(contentUri: String, isFavorite: Boolean): Int

    @Query("SELECT contentUri FROM photo_refs WHERE isFavorite = 1")
    fun getFavoritePhotoUrisFlow(): Flow<List<String>>

    @Query("SELECT contentUri FROM photo_refs WHERE isFavorite = 1")
    fun getFavoritePhotoUrisSync(): List<String>

    @Query("SELECT * FROM photo_refs WHERE isFavorite = 1 ORDER BY takenAt DESC")
    fun getFavoritePhotosFlow(): Flow<List<PhotoRefEntity>>

    @Query("DELETE FROM photo_refs WHERE id = :photoId")
    fun deletePhotoById(photoId: Long): Int
}
