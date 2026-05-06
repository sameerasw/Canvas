package com.sameerasw.canvas.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "canvas")
data class CanvasEntity(
    @PrimaryKey val id: Int = 1,
    val contentJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

