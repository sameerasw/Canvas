package com.sameerasw.doodlist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CanvasDao {
    @Query("SELECT * FROM canvas WHERE id = 1 LIMIT 1")
    suspend fun getCanvas(): CanvasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(canvas: CanvasEntity)

    @Query("DELETE FROM canvas")
    suspend fun clear()
}

