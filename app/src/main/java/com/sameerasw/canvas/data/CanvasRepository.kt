package com.sameerasw.canvas.data

import android.content.Context
import com.google.gson.Gson
import com.sameerasw.canvas.util.GsonProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CanvasRepository(context: Context) {
    private val db = AppDatabase.getInstance(context.applicationContext)
    private val dao = db.canvasDao()
    private val gson = GsonProvider.create()

    suspend fun loadCanvas(): String? = withContext(Dispatchers.IO) {
        dao.getCanvas()?.contentJson
    }

    suspend fun saveCanvas(json: String) = withContext(Dispatchers.IO) {
        dao.upsert(CanvasEntity(contentJson = json))
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dao.clear()
    }
}
