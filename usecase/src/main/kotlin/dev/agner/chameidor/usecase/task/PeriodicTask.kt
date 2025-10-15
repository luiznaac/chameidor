package dev.agner.chameidor.usecase.task

import kotlinx.datetime.LocalDateTime
import org.springframework.scheduling.support.CronExpression

data class PeriodicTaskContext(
    val id: Int,
    val definition: PeriodicTaskDefinition,
    val status: TaskStatus,
    val nextExecution: LocalDateTime,
    val lastExecution: LocalDateTime? = null,
)

data class PeriodicTaskDefinition(
    val cron: CronExpression,
    val message: String,
)

enum class TaskStatus {
    WAITING,
    EXECUTING,
}
