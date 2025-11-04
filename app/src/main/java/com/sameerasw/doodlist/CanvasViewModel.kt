package com.sameerasw.doodlist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.doodlist.data.CanvasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CanvasRepository(application)
    private val gson = Gson()

    private val _strokes = MutableStateFlow<List<DrawStroke>>(emptyList())
    val strokes = _strokes.asStateFlow()

    init {
        load()
    }

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

    fun save() {
        viewModelScope.launch {
            val json = gson.toJson(_strokes.value)
            repository.saveCanvas(json)
        }
    }

    private fun load() {
        viewModelScope.launch {
            val json = repository.loadCanvas()
            if (json != null) {
                try {
                    val type = object : TypeToken<List<DrawStroke>>() {}.type
                    val list: List<DrawStroke> = gson.fromJson(json, type)
                    _strokes.value = list
                } catch (e: Exception) {
                    // ignore parse errors
                }
            }
        }
    }
}

