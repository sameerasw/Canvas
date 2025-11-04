package com.sameerasw.canvas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "canvas")
data class CanvasEntity(
    @PrimaryKey val id: Int = 1,
    val contentJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

