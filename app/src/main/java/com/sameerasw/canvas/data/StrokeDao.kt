package com.sameerasw.canvas.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StrokeDao {
    @Query("SELECT * FROM strokes WHERE taskId = :taskId ORDER BY id ASC")
    fun getForTask(taskId: Long): Flow<List<StrokeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(stroke: StrokeEntity): Long

    @Query("DELETE FROM strokes WHERE taskId = :taskId")
    fun deleteForTask(taskId: Long): Int
}
