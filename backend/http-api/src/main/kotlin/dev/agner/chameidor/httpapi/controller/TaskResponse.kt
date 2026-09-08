package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.task.TaskExecutionView
import dev.agner.chameidor.usecase.task.TaskHistoryEntry
import dev.agner.chameidor.usecase.task.TaskView
import kotlinx.datetime.LocalDateTime

/**
 * Edge DTOs for the task read endpoints. `frontend/src/api/types.ts` is a
 * hand-maintained mirror of this file — change both in the same commit.
 *
 * Serialized by `JsonMapper`: property names become snake_case, null fields are
 * omitted, dates are ISO-8601 strings.
 */
data class TaskResponse(
    val id: Int,
    val type: String,
    val host: String,
    val endpoint: String,
    val data: Any?,
    val cron: String?,
    val status: String,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val executedAt: LocalDateTime?,
    val nextExecutionAt: LocalDateTime?,
) {
    companion object {
        private const val ONE_TIME = "one_time"
        private const val PERIODIC = "periodic"

        fun from(view: TaskView): TaskResponse = TaskResponse(
            id = view.id,
            type = if (view.isPeriodic) PERIODIC else ONE_TIME,
            host = view.host,
            endpoint = view.endpoint,
            data = view.data,
            cron = view.cron?.toString()?.removePrefix("0 "),
            status = view.status.name,
            createdBy = view.createdBy,
            createdAt = view.createdAt,
            executedAt = view.executedAt,
            nextExecutionAt = view.nextExecutionAt,
        )
    }
}

data class TaskExecutionResponse(
    val id: Int,
    val taskId: Int,
    val executedAt: LocalDateTime,
    val durationMs: Long,
    val status: String,
    val result: Any?,
) {
    companion object {
        fun from(view: TaskExecutionView): TaskExecutionResponse = TaskExecutionResponse(
            id = view.id,
            taskId = view.taskId,
            executedAt = view.executedAt,
            durationMs = view.durationMs,
            status = view.status,
            result = view.result,
        )
    }
}

data class TaskHistoryResponse(
    val status: String,
    val executedAt: LocalDateTime?,
    val nextExecutionAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(entry: TaskHistoryEntry): TaskHistoryResponse = TaskHistoryResponse(
            status = entry.status,
            executedAt = entry.executedAt,
            nextExecutionAt = entry.nextExecutionAt,
            createdAt = entry.createdAt,
        )
    }
}
