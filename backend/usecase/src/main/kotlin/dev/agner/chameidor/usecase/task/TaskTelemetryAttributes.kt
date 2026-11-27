package dev.agner.chameidor.usecase.task

/**
 * The background-work correlation attributes from `docs/observability.md` §3. They live in the
 * task package because this lane, not the scaffold, owns the task loop; the shared
 * `TelemetryAttributes` stays the scaffold-owned set.
 */
object TaskTelemetryAttributes {

    const val JOB_ID = "job.id"
    const val TASK_ID = "task.id"
}
