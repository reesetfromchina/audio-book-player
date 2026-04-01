package com.ive.audiobookplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AudiobookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun audiobookDao(): AudiobookDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object{

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object  : Migration(1,2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE chapters ADD COLUMN startMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context) : AppDatabase{
            return INSTANCE ?: synchronized(this){
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audiobook_db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}