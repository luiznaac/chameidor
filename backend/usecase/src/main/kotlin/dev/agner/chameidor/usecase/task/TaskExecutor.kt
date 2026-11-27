package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.EventLevel
import dev.agner.chameidor.usecase.commons.TelemetryAttributes
import dev.agner.chameidor.usecase.commons.event
import dev.agner.chameidor.usecase.commons.logger
import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.Task.PeriodicTask
import dev.agner.chameidor.usecase.task.TaskStatus.EXECUTING
import dev.agner.chameidor.usecase.task.TaskTelemetryAttributes.JOB_ID
import dev.agner.chameidor.usecase.task.TaskTelemetryAttributes.TASK_ID
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.measureTimedValue

@Service
class TaskExecutor(
    private val clock: Clock,
    private val callClient: CallClient,
    private val taskRepository: ITaskRepository,
    private val taskExecutionRepository: ITaskExecutionRepository,
) {

    private val log = logger()

    suspend fun execute(task: Task, jobId: String) = with(task) {
        val now = LocalDateTime.now(clock)
        taskRepository.updateStatus(id, EXECUTING)

        runTask(task, jobId).also {
            taskExecutionRepository.save(task.id, now, it)
        }

        taskRepository.updateExecutedAt(id, now)

        if (this is PeriodicTask) {
            val nextExecutionAt = now.nextBy(cron)
            taskRepository.updateNextExecution(id, nextExecutionAt)
        }
    }

    private suspend fun runTask(task: Task, jobId: String): TaskExecutionResult {
        val exec = measureTimedValue {
            runCatching {
                callClient.makeCall(task)
            }
        }

        val failure = exec.value.exceptionOrNull()
        return if (failure == null) {
            TaskExecutionResult.Success(exec.duration, exec.value.getOrNull())
        } else {
            log.event(
                EXECUTION_FAILED,
                JOB_ID to jobId,
                TASK_ID to task.id,
                TelemetryAttributes.ERROR_TYPE to failure::class.java.simpleName,
                level = EventLevel.ERROR,
                throwable = failure,
            )
            TaskExecutionResult.Failure(exec.duration, failure.message ?: "Unknown error")
        }
    }

    companion object {
        private const val EXECUTION_FAILED = "tasks.execution.failed"
    }
}

private fun LocalDateTime.nextBy(cron: CronExpression) = cron.next(toJavaLocalDateTime())!!.toKotlinLocalDateTime()

sealed class TaskExecutionResult(
    val duration: Duration,
) {
    class Success(duration: Duration, val response: Any?) : TaskExecutionResult(duration)
    class Failure(duration: Duration, val message: String) : TaskExecutionResult(duration)
}
