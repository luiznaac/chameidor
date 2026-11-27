package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.EventLevel
import dev.agner.chameidor.usecase.commons.RequestContext
import dev.agner.chameidor.usecase.commons.TelemetryAttributes
import dev.agner.chameidor.usecase.commons.event
import dev.agner.chameidor.usecase.commons.logger
import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.TaskStatus.QUEUED
import dev.agner.chameidor.usecase.task.TaskTelemetryAttributes.JOB_ID
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(DelicateCoroutinesApi::class, ExperimentalTime::class)
@Service
class TaskService(
    private val clock: Clock,
    private val repository: ITaskRepository,
    private val taskExecutor: TaskExecutor,
    @param:Value("\${enqueue.period}") private val enqueuePeriod: Long,
) : DisposableBean {

    private val logger = logger()
    private val schedulerJobId = RequestContext.newId()
    private val globalJob: Job

    init {
        globalJob = GlobalScope.launch { execute() }
    }

    // The loop must not outlive the context that owns it — a stale loop would keep polling and
    // executing tasks with beans (HTTP client, clock) that were already shut down with it.
    override fun destroy() {
        globalJob.cancel()
    }

    suspend fun execute() {
        delay(5.seconds)

        while (true) {
            val execDuration = enqueueOnce()

            val waitFor = max(enqueuePeriod.seconds.inWholeMilliseconds - execDuration.inWholeMilliseconds, 0L).also {
                if (it == 0L) {
                    logger.event(
                        ENQUEUE_SLOW,
                        JOB_ID to schedulerJobId,
                        ENQUEUE_PERIOD_SECONDS to enqueuePeriod,
                        ENQUEUE_DURATION_MILLIS to execDuration.inWholeMilliseconds,
                        level = EventLevel.WARN,
                    )
                }
            }
            delay(waitFor.milliseconds)
        }
    }

    suspend fun register(creation: TaskCreation, externalSystem: String) =
        repository.createTask(creation, externalSystem)

    suspend fun isRunning() = globalJob.isActive

    internal suspend fun enqueueOnce(): kotlin.time.Duration = measureTime {
        runCatching { enqueueTasks() }
            .onFailure {
                logger.event(
                    ENQUEUE_FAILED,
                    JOB_ID to schedulerJobId,
                    TelemetryAttributes.ERROR_TYPE to it::class.java.simpleName,
                    level = EventLevel.ERROR,
                    throwable = it,
                )
            }
    }

    internal suspend fun enqueueTasks() {
        val now = LocalDateTime.now(clock)

        repository.findExecutableTasks(now)
            .onEach {
                repository.updateStatus(it.id, QUEUED)
                val jobId = RequestContext.newId()
                GlobalScope.launch {
                    taskExecutor.execute(it, jobId)
                }
            }
    }

    companion object {
        private const val ENQUEUE_FAILED = "tasks.enqueue.failed"
        private const val ENQUEUE_SLOW = "tasks.enqueue.slow"
        private const val ENQUEUE_PERIOD_SECONDS = "enqueue.period.seconds"
        private const val ENQUEUE_DURATION_MILLIS = "enqueue.duration.ms"
    }
}
