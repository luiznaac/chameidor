package dev.agner.chameidor.usecase.task

import kotlinx.datetime.LocalDateTime

interface ITaskRepository {

    suspend fun createTask(task: TaskCreation, createdBy: String): Task

    suspend fun findExecutableTasks(now: LocalDateTime): List<Task>

    suspend fun updateStatus(id: Int, status: TaskStatus)

    suspend fun updateExecutedAt(id: Int, executedAt: LocalDateTime)

    suspend fun updateNextExecution(id: Int, nextExecutionAt: LocalDateTime)

    suspend fun findAll(filter: TaskFilter): List<TaskView>

    suspend fun findViewById(id: Int): TaskView?

    suspend fun findHistory(taskId: Int): List<TaskHistoryEntry>
}
