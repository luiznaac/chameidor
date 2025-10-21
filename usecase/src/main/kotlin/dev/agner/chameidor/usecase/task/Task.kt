package dev.agner.chameidor.usecase.task

import org.springframework.scheduling.support.CronExpression

sealed class Task(
    val id: Int,
    val host: String,
    val endpoint: String,
    val data: Any?,
) {
    class PeriodicTask(
        id: Int,
        host: String,
        endpoint: String,
        data: Any?,
        val cron: CronExpression,
    ) : Task(id, host, endpoint, data)

    class OneTimeTask(
        id: Int,
        host: String,
        endpoint: String,
        data: Any?,
    ) : Task(id, host, endpoint, data)
}

enum class TaskStatus {
    WAITING,
    QUEUED,
    EXECUTING,
    EXECUTED,
}
