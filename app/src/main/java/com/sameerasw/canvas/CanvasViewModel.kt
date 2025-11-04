package com.sameerasw.canvas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.canvas.data.CanvasRepository
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.model.DrawStroke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CanvasModel(
    val strokes: List<DrawStroke> = emptyList(),
    val texts: List<TextItem> = emptyList()
)

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CanvasRepository(application)
    private val gson = Gson()

    private val _strokes = MutableStateFlow<List<DrawStroke>>(emptyList())
    val strokes = _strokes.asStateFlow()

    private val _texts = MutableStateFlow<List<TextItem>>(emptyList())
    val texts = _texts.asStateFlow()

    init {
        load()
    }

    // Stroke operations
    fun setStrokes(list: List<DrawStroke>) {
        _strokes.value = list
    }

    fun addStroke(stroke: DrawStroke) {
        _strokes.value = _strokes.value + stroke
    }

    fun removeStroke(predicate: (DrawStroke) -> Boolean) {
        _strokes.value = _strokes.value.filterNot { predicate(it) }
    }

    fun clearStrokes() {
        _strokes.value = emptyList()
    }

    // Text operations
    fun addText(textItem: TextItem) {
        _texts.value = _texts.value + textItem
    }

    fun updateText(updated: TextItem) {
        _texts.value = _texts.value.map { if (it.id == updated.id) updated else it }
    }

    fun removeText(id: Long) {
        _texts.value = _texts.value.filterNot { it.id == id }
    }

    fun clearTexts() {
        _texts.value = emptyList()
    }

    fun clearAll() {
        clearStrokes()
        clearTexts()
    }

    fun save() {
        viewModelScope.launch {
            val model = CanvasModel(_strokes.value, _texts.value)
            val json = gson.toJson(model)
            repository.saveCanvas(json)
        }
    }

    private fun load() {
        viewModelScope.launch {
            val json = repository.loadCanvas()
            if (json != null) {
                try {
                    val type = object : TypeToken<CanvasModel>() {}.type
                    val model: CanvasModel = gson.fromJson(json, type)
                    _strokes.value = model.strokes
                    _texts.value = model.texts
                } catch (_: Exception) {
                    // handle old format: maybe only strokes were saved
                    try {
                        val type = object : TypeToken<List<DrawStroke>>() {}.type
                        val list: List<DrawStroke> = gson.fromJson(json, type)
                        _strokes.value = list
                    } catch (_: Exception) {
                        // ignore
                    }
                }
            }
        }
    }
}
