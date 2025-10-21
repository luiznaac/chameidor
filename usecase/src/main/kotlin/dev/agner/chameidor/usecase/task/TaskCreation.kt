package dev.agner.chameidor.usecase.task

import org.springframework.scheduling.support.CronExpression

sealed class TaskCreation(
    val host: String,
    val endpoint: String,
    val data: Any?,
) {
    class PeriodicTaskCreation(
        host: String,
        endpoint: String,
        data: Any?,
        val cron: CronExpression,
    ) : TaskCreation(host, endpoint, data)

    class OneTimeTaskCreation(
        host: String,
        endpoint: String,
        data: Any?,
    ) : TaskCreation(host, endpoint, data)
}
