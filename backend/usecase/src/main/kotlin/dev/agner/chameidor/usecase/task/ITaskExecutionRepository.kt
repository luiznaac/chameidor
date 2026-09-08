package dev.agner.chameidor.usecase.task

import kotlinx.datetime.LocalDateTime

interface ITaskExecutionRepository {
    suspend fun save(taskId: Int, executedAt: LocalDateTime, taskExecutionResult: TaskExecutionResult)

    suspend fun findByTaskId(taskId: Int, limit: Int, offset: Int): List<TaskExecutionView>

    suspend fun findRecent(limit: Int): List<TaskExecutionView>
}
