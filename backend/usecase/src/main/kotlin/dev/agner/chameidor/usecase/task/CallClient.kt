package dev.agner.chameidor.usecase.task

/**
 * Callback wire headers of `POST http://{task.host}{task.endpoint}` (contracts/README.md): the
 * system that registered the task, and chameidor's own task id. The Bearer credential that
 * authenticates the call is chameidor's identity token, see `docs/external-systems.md`.
 */
const val EXTERNAL_SYSTEM_HEADER = "X-External-System"
const val TASK_ID_HEADER = "X-Chameidor-Task-Id"

interface CallClient {

    suspend fun makeCall(task: Task): Any?
}
