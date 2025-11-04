package com.sameerasw.canvas.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Repository(private val db: AppDatabase) {
    fun getTasks(): Flow<List<TaskEntity>> = db.taskDao().getAll()

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        db.taskDao().insert(task)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        db.taskDao().update(task)
    }

    suspend fun deleteTask(id: Long) = withContext(Dispatchers.IO) {
        db.taskDao().delete(id)
    }

    fun getStrokesForTask(taskId: Long): Flow<List<StrokeEntity>> = db.strokeDao().getForTask(taskId)

    suspend fun insertStroke(stroke: StrokeEntity): Long = withContext(Dispatchers.IO) {
        db.strokeDao().insert(stroke)
    }

    suspend fun deleteStrokesForTask(taskId: Long) = withContext(Dispatchers.IO) {
        db.strokeDao().deleteForTask(taskId)
    }
}
