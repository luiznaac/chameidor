package dev.agner.chameidor.usecase.task

import org.springframework.scheduling.support.CronExpression

sealed class Task(
    val id: Int,
    val host: String,
    val endpoint: String,
    val data: Any?,
    val createdBy: String,
) {
    class PeriodicTask(
        id: Int,
        host: String,
        endpoint: String,
        data: Any?,
        createdBy: String,
        val cron: CronExpression,
    ) : Task(id, host, endpoint, data, createdBy)

    class OneTimeTask(
        id: Int,
        host: String,
        endpoint: String,
        data: Any?,
        createdBy: String,
    ) : Task(id, host, endpoint, data, createdBy)
}

enum class TaskStatus {
    WAITING,
    QUEUED,
    EXECUTING,
    EXECUTED,
}
