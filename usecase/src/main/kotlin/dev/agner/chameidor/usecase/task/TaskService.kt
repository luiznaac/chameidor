package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.logger
import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.TaskStatus.QUEUED
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
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
) {

    private val logger = logger()
    private val globalJob: Job

    init {
        globalJob = GlobalScope.launch { execute() }
    }

    suspend fun execute() {
        delay(5.seconds)

        while (true) {
            val execDuration = measureTime {
                runCatching { enqueueTasks() }
                    .onFailure { logger.error("Error enqueuing tasks", it) }
            }

            val waitFor = max(enqueuePeriod.seconds.inWholeMilliseconds - execDuration.inWholeMilliseconds, 0L).also {
                if (it == 0L) logger.warn("Enqueuing took longer than $enqueuePeriod seconds")
            }
            delay(waitFor.milliseconds)
        }
    }

    suspend fun register(creation: TaskCreation, externalSystem: String) =
        repository.createTask(creation, externalSystem)

    suspend fun isRunning() = globalJob.isActive

    private suspend fun enqueueTasks() {
        val now = LocalDateTime.now(clock)

        repository.findExecutableTasks(now)
            .onEach {
                repository.updateStatus(it.id, QUEUED)
                GlobalScope.launch {
                    taskExecutor.execute(it)
                }
            }
    }
}
