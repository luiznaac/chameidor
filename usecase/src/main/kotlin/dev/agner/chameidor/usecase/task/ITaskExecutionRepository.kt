package dev.agner.chameidor.usecase.task

import kotlinx.datetime.LocalDateTime

interface ITaskExecutionRepository {
    suspend fun save(taskId: Int, executedAt: LocalDateTime, taskExecutionResult: TaskExecutionResult)
}
