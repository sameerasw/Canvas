package com.sameerasw.canvas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import com.sameerasw.canvas.data.CanvasRepository
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.model.DrawStroke
import com.sameerasw.canvas.util.GsonProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CanvasModel(
    val strokes: List<DrawStroke> = emptyList(),
    val texts: List<TextItem> = emptyList()
)

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CanvasRepository(application)
    private val gson = GsonProvider.create()

    private val _strokes = MutableStateFlow<List<DrawStroke>>(emptyList())
    val strokes = _strokes.asStateFlow()

    private val _texts = MutableStateFlow<List<TextItem>>(emptyList())
    val texts = _texts.asStateFlow()

    // Notes Role state
    private val _stylusMode = MutableStateFlow(false)
    val stylusMode = _stylusMode.asStateFlow()

    private val _launchedFromLockScreen = MutableStateFlow(false)
    val launchedFromLockScreen = _launchedFromLockScreen.asStateFlow()

    private val _isFloatingWindow = MutableStateFlow(false)
    val isFloatingWindow = _isFloatingWindow.asStateFlow()

    private val _isNotesRoleHeld = MutableStateFlow(false)
    val isNotesRoleHeld = _isNotesRoleHeld.asStateFlow()

    // Undo/Redo stacks hold CanvasModel snapshots
    private val undoStack = ArrayDeque<CanvasModel>()
    private val redoStack = ArrayDeque<CanvasModel>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    init {
        load()
    }

    private fun pushUndoSnapshot() {
        // capture current state
        undoStack.addLast(CanvasModel(_strokes.value, _texts.value))
        // cap stack size to reasonable limit (e.g., 50)
        if (undoStack.size > 50) undoStack.removeFirst()
        _canUndo.value = undoStack.isNotEmpty()
        // any new action clears redo stack
        if (redoStack.isNotEmpty()) {
            redoStack.clear()
            _canRedo.value = false
        }
    }

    // Stroke operations
    fun setStrokes(list: List<DrawStroke>) {
        pushUndoSnapshot()
        _strokes.value = list
    }

    fun addStroke(stroke: DrawStroke) {
        pushUndoSnapshot()
        _strokes.value = _strokes.value + stroke
    }

    fun removeStroke(predicate: (DrawStroke) -> Boolean) {
        pushUndoSnapshot()
        _strokes.value = _strokes.value.filterNot { predicate(it) }
    }

    fun clearStrokes() {
        pushUndoSnapshot()
        _strokes.value = emptyList()
    }

    // Text operations
    fun addText(textItem: TextItem) {
        pushUndoSnapshot()
        _texts.value = _texts.value + textItem
    }

    fun updateText(updated: TextItem) {
        pushUndoSnapshot()
        _texts.value = _texts.value.map { if (it.id == updated.id) updated else it }
    }

    fun removeText(id: Long) {
        pushUndoSnapshot()
        _texts.value = _texts.value.filterNot { it.id == id }
    }

    fun clearTexts() {
        pushUndoSnapshot()
        _texts.value = emptyList()
    }

    fun clearAll() {
        pushUndoSnapshot()
        // Directly clear both lists to avoid double-pushing undo snapshots
        _strokes.value = emptyList()
        _texts.value = emptyList()
    }

    fun invert() {
        pushUndoSnapshot()
        
        fun invertColor(color: androidx.compose.ui.graphics.Color): androidx.compose.ui.graphics.Color {
            val r = color.red
            val g = color.green
            val b = color.blue
            val max = maxOf(r, maxOf(g, b))
            val min = minOf(r, minOf(g, b))
            return if (max - min < 0.1f) {
                androidx.compose.ui.graphics.Color(1f - r, 1f - g, 1f - b, color.alpha)
            } else {
                color
            }
        }

        _strokes.value = _strokes.value.map { stroke ->
            stroke.copy(color = invertColor(stroke.color))
        }
        
        _texts.value = _texts.value.map { text ->
            text.copy(color = invertColor(text.color))
        }
    }

    // Undo/Redo operations
    fun undo() {
        if (undoStack.isEmpty()) return
        // push current state to redo
        redoStack.addLast(CanvasModel(_strokes.value, _texts.value))
        _canRedo.value = redoStack.isNotEmpty()
        // restore previous state
        val prev = undoStack.removeLast()
        _strokes.value = prev.strokes
        _texts.value = prev.texts
        _canUndo.value = undoStack.isNotEmpty()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        // push current state to undo
        undoStack.addLast(CanvasModel(_strokes.value, _texts.value))
        _canUndo.value = undoStack.isNotEmpty()
        // restore next state
        val next = redoStack.removeLast()
        _strokes.value = next.strokes
        _texts.value = next.texts
        _canRedo.value = redoStack.isNotEmpty()
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

    // Notes Role methods
    fun setStylusMode(enabled: Boolean) {
        _stylusMode.value = enabled
    }

    fun setLaunchedFromLockScreen(isLockScreen: Boolean) {
        _launchedFromLockScreen.value = isLockScreen
    }

    fun setIsFloatingWindow(isFloating: Boolean) {
        _isFloatingWindow.value = isFloating
    }

    fun setIsNotesRoleHeld(held: Boolean) {
        _isNotesRoleHeld.value = held
    }
}
