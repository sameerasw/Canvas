package com.sameerasw.canvas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Simple serialized stroke stored as a string of x,y pairs separated by ;
@Entity(tableName = "strokes")
data class StrokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val pathData: String // e.g. "x1,y1;x2,y2;..."
)

