package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.logger
import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.Task.PeriodicTask
import dev.agner.chameidor.usecase.task.TaskStatus.EXECUTING
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
    private val logger = logger()

    suspend fun execute(task: Task) = with(task) {
        val now = LocalDateTime.now(clock)
        logger.info("Executing task $id at $now")
        taskRepository.updateStatus(id, EXECUTING)

        runTask(task).also { taskExecutionRepository.save(task.id, now, it) }

        taskRepository.updateExecutedAt(id, now)

        if (this is PeriodicTask) {
            logger.info("Updating next execution for task $id")
            val nextExecutionAt = now.nextBy(cron)
            taskRepository.updateNextExecution(id, nextExecutionAt)
        }
    }

    private suspend fun runTask(task: Task) = with(task) {
        val exec = measureTimedValue {
            runCatching {
                callClient.makeCall(host, endpoint, data)
            }
        }

        if (exec.value.isSuccess) {
            TaskExecutionResult.Success(exec.duration, exec.value.getOrNull())
        } else {
            TaskExecutionResult.Failure(exec.duration, exec.value.exceptionOrNull()!!)
        }
    }
}

private fun LocalDateTime.nextBy(cron: CronExpression) = cron.next(toJavaLocalDateTime())!!.toKotlinLocalDateTime()

sealed class TaskExecutionResult(
    val duration: Duration,
) {
    class Success(duration: Duration, val response: Any?) : TaskExecutionResult(duration)
    class Failure(duration: Duration, val exception: Throwable) : TaskExecutionResult(duration)
}
