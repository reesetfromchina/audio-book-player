package com.ive.audiobookplayer.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ive.audiobookplayer.database.daos.AudiobookDao
import com.ive.audiobookplayer.database.daos.BookmarkDao
import com.ive.audiobookplayer.database.daos.NoteDao
import com.ive.audiobookplayer.database.entity.AudiobookEntity
import com.ive.audiobookplayer.database.entity.BookmarkEntity
import com.ive.audiobookplayer.database.entity.ChapterEntity
import com.ive.audiobookplayer.database.entity.NoteEntity

@Database(
    entities = [
        AudiobookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase(){
    abstract fun audiobookDao(): AudiobookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao() : NoteDao

    companion object{

        @Volatile
        private var INSTANCE: AppDatabase? = null

//        val MIGRATION_1_2 = object  : Migration(1,2){
//            override fun migrate(database: SupportSQLiteDatabase) {
//                database.execSQL("ALTER TABLE chapters ADD COLUMN startMs INTEGER NOT NULL DEFAULT 0")
//            }
//        }

        fun getInstance(context: Context) : AppDatabase{
            return INSTANCE ?: synchronized(this){
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "audiobook_db"
                )
                    .build().also { INSTANCE = it }
            }
        }
    }
}