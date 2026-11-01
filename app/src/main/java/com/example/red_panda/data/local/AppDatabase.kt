package com.example.red_panda.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.red_panda.data.local.dao.CategoryDao
import com.example.red_panda.data.local.dao.EventDao
import com.example.red_panda.data.local.dao.PhotoDao
import com.example.red_panda.data.local.entity.CategoryEntity
import com.example.red_panda.data.local.entity.EventEntity
import com.example.red_panda.data.local.entity.EventPhotoEntity
import com.example.red_panda.data.local.entity.PhotoRefEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        EventEntity::class,
        PhotoRefEntity::class,
        EventPhotoEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun eventDao(): EventDao
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE categories ADD COLUMN coverPhotoIsManual INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE events ADD COLUMN coverPhotoIsManual INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE event_photos ADD COLUMN position INTEGER NOT NULL DEFAULT 0"
                )

                db.execSQL(
                    """
                    UPDATE event_photos
                    SET position = (
                        SELECT COUNT(*) - 1
                        FROM event_photos AS earlier
                        JOIN photo_refs AS earlier_photo ON earlier.photoId = earlier_photo.id
                        JOIN photo_refs AS current_photo ON current_photo.id = event_photos.photoId
                        WHERE earlier.eventId = event_photos.eventId
                          AND (
                            earlier_photo.takenAt > current_photo.takenAt
                            OR (
                                earlier_photo.takenAt = current_photo.takenAt
                                AND earlier.photoId <= event_photos.photoId
                            )
                          )
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE categories
                    SET coverPhotoIsManual = 1
                    WHERE coverPhotoId IS NOT NULL
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE categories
                    SET coverPhotoId = (
                        SELECT p.id
                        FROM photo_refs AS p
                        JOIN event_photos AS ep ON ep.photoId = p.id
                        JOIN events AS e ON e.id = ep.eventId
                        WHERE e.categoryId = categories.id
                        ORDER BY p.takenAt DESC, p.id
                        LIMIT 1
                    )
                    WHERE coverPhotoId IS NULL
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE events
                    SET coverPhotoId = (
                        SELECT ep.photoId
                        FROM event_photos AS ep
                        JOIN photo_refs AS p ON p.id = ep.photoId
                        WHERE ep.eventId = events.id
                        ORDER BY ep.position
                        LIMIT 1
                    )
                    WHERE coverPhotoId IS NULL
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "red_panda_gallery.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
