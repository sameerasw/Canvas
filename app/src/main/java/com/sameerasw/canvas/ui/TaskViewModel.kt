package com.sameerasw.canvas.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.canvas.data.AppDatabase
import com.sameerasw.canvas.data.Repository
import com.sameerasw.canvas.data.StrokeEntity
import com.sameerasw.canvas.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = Repository(db)

    val tasks: StateFlow<List<TaskEntity>> = repo.getTasks()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addTask(text: String) = viewModelScope.launch {
        repo.insertTask(TaskEntity(text = text))
    }

    fun updateTask(task: TaskEntity) = viewModelScope.launch {
        repo.updateTask(task)
    }

    fun markDone(id: Long) = viewModelScope.launch {
        // fetch the current entity from the state flow and update
        val task = tasks.value.firstOrNull { it.id == id } ?: return@launch
        repo.updateTask(task.copy(isDone = true))
    }

    fun markUndone(id: Long) = viewModelScope.launch {
        // fetch the current entity from the state flow, update, and remove strokes
        val task = tasks.value.firstOrNull { it.id == id } ?: return@launch
        repo.updateTask(task.copy(isDone = false))
        repo.deleteStrokesForTask(id)
    }

    fun deleteTask(id: Long) = viewModelScope.launch {
        // remove strokes for the task then delete the task
        repo.deleteStrokesForTask(id)
        repo.deleteTask(id)
    }

    fun saveStroke(taskId: Long, pathData: String) = viewModelScope.launch {
        // delete old strokes and store this one (for simplicity store last stroke only)
        repo.deleteStrokesForTask(taskId)
        repo.insertStroke(StrokeEntity(taskId = taskId, pathData = pathData))
    }

    fun getStrokesFlow(taskId: Long) = repo.getStrokesForTask(taskId)
}
