package com.sameerasw.canvas.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [CanvasEntity::class, TaskEntity::class, StrokeEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun canvasDao(): CanvasDao
    abstract fun taskDao(): TaskDao
    abstract fun strokeDao(): StrokeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "doodlist-db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
