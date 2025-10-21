package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.logger
import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.TaskStatus.QUEUED
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
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
) {

    private val logger = logger()

    init {
        GlobalScope.launch { execute() }
    }

    suspend fun execute() {
        delay(5.seconds)

        while (true) {
            val execDuration = measureTime {
                val now = LocalDateTime.now(clock)
                logger.info("Finding executable tasks at $now")

                val taskCount = repository.findExecutableTasks(now)
                    .onEach {
                        logger.info("Queuing task ${it.id}")
                        repository.updateStatus(it.id, QUEUED)
                        GlobalScope.launch {
                            taskExecutor.execute(it)
                        }
                    }
                    .count()

                logger.info("Found $taskCount tasks to execute")
            }

            // Calculate wait time so every execution happens exactly every 10 seconds (except when it takes longer)
            val waitFor = max(10.seconds.inWholeMilliseconds - execDuration.inWholeMilliseconds, 0)
            logger.info("Will wait $waitFor milliseconds before executing next batch of tasks")
            delay(waitFor.milliseconds)
        }
    }

    suspend fun register(creation: TaskCreation, externalSystem: String) =
        repository.createTask(creation, externalSystem)
}
