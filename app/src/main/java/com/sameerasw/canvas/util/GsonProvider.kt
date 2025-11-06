package com.sameerasw.canvas.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonProvider {
    fun create(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Color::class.java, ColorTypeAdapter())
            .registerTypeAdapter(Offset::class.java, OffsetTypeAdapter())
            .registerTypeAdapter(com.sameerasw.canvas.model.StylusPoint::class.java, StylusPointTypeAdapter())
            .create()
    }
}
