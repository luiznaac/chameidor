package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.task.Task

/**
 * Edge DTO for the `201 Created` body of `POST /tasks/one-time` and `POST /tasks/periodic`
 * (fixtures: `contracts/tasks-one-time-response.json`, `contracts/tasks-periodic-response.json`).
 * Only what the caller registered is echoed; the full task view is the read endpoints'
 * [TaskResponse].
 */
data class TaskRegistrationResponse(
    val id: Int,
    val host: String,
    val endpoint: String,
    val data: Any?,
    val cron: String?,
) {
    companion object {
        fun from(task: Task): TaskRegistrationResponse = TaskRegistrationResponse(
            id = task.id,
            host = task.host,
            endpoint = task.endpoint,
            data = task.data,
            cron = (task as? Task.PeriodicTask)?.cron?.toString()?.removePrefix("0 "),
        )
    }
}
