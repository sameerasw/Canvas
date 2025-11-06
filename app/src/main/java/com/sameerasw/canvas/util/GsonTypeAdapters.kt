package com.sameerasw.canvas.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class ColorTypeAdapter : TypeAdapter<Color>() {
    override fun write(out: JsonWriter, value: Color?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.value.toLong())
        }
    }

    override fun read(input: JsonReader): Color? {
        if (input.peek() == com.google.gson.stream.JsonToken.NULL) {
            input.nextNull()
            return null
        }
        val colorValue = input.nextLong()
        return Color(colorValue.toULong())
    }
}

class OffsetTypeAdapter : TypeAdapter<Offset>() {
    override fun write(out: JsonWriter, value: Offset?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.beginObject()
            out.name("x").value(value.x.toDouble())
            out.name("y").value(value.y.toDouble())
            out.endObject()
        }
    }

    override fun read(input: JsonReader): Offset? {
        if (input.peek() == com.google.gson.stream.JsonToken.NULL) {
            input.nextNull()
            return null
        }
        var x = 0f
        var y = 0f
        input.beginObject()
        while (input.hasNext()) {
            when (input.nextName()) {
                "x" -> x = input.nextDouble().toFloat()
                "y" -> y = input.nextDouble().toFloat()
                else -> input.skipValue()
            }
        }
        input.endObject()
        return Offset(x, y)
    }
}

class StylusPointTypeAdapter : TypeAdapter<com.sameerasw.canvas.model.StylusPoint>() {
    override fun write(out: JsonWriter, value: com.sameerasw.canvas.model.StylusPoint?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.beginObject()
            out.name("x").value(value.offset.x.toDouble())
            out.name("y").value(value.offset.y.toDouble())
            out.name("p").value(value.pressure.toDouble())
            out.name("t").value(value.tilt.toDouble())
            out.name("o").value(value.orientation.toDouble())
            out.endObject()
        }
    }

    override fun read(input: JsonReader): com.sameerasw.canvas.model.StylusPoint? {
        if (input.peek() == com.google.gson.stream.JsonToken.NULL) {
            input.nextNull()
            return null
        }
        var x = 0f
        var y = 0f
        var pressure = 1f
        var tilt = 0f
        var orientation = 0f
        input.beginObject()
        while (input.hasNext()) {
            when (input.nextName()) {
                "x" -> x = input.nextDouble().toFloat()
                "y" -> y = input.nextDouble().toFloat()
                "p" -> pressure = input.nextDouble().toFloat()
                "t" -> tilt = input.nextDouble().toFloat()
                "o" -> orientation = input.nextDouble().toFloat()
                else -> input.skipValue()
            }
        }
        input.endObject()
        return com.sameerasw.canvas.model.StylusPoint(Offset(x, y), pressure, tilt, orientation)
    }
}
