package dev.agner.chameidor.usecase.task

import kotlinx.datetime.LocalDateTime
import org.springframework.scheduling.support.CronExpression

/**
 * Read model for a persisted task. Unlike [Task] (which only describes *what to
 * call*), this carries the scheduling metadata the repository stores — status,
 * timestamps and the owning external system. A one-time task has a null [cron];
 * a periodic one carries its schedule. Mapped to a `*Response` DTO at the HTTP
 * edge and never leaves the read path.
 */
data class TaskView(
    val id: Int,
    val host: String,
    val endpoint: String,
    val data: Any?,
    val cron: CronExpression?,
    val status: TaskStatus,
    val createdBy: String,
    val createdAt: LocalDateTime,
    val executedAt: LocalDateTime?,
    val nextExecutionAt: LocalDateTime?,
) {
    val isPeriodic: Boolean get() = cron != null
}

/** One recorded run of a task's target endpoint. */
data class TaskExecutionView(
    val id: Int,
    val taskId: Int,
    val executedAt: LocalDateTime,
    val durationMs: Long,
    val status: String,
    val result: Any?,
)

/** One audit snapshot of a task, written on creation and after every mutation. */
data class TaskHistoryEntry(
    val status: String,
    val executedAt: LocalDateTime?,
    val nextExecutionAt: LocalDateTime?,
    val createdAt: LocalDateTime,
)

/** Optional filters for listing tasks. */
data class TaskFilter(
    val status: TaskStatus? = null,
    val createdBy: String? = null,
    val limit: Int = DEFAULT_LIMIT,
    val offset: Int = 0,
) {
    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
