package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.TaskStatus.WAITING
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

@OptIn(DelicateCoroutinesApi::class)
@Service
class PeriodicTaskService(
    private val clock: Clock,
) {

    init {
        GlobalScope.launch { execute() }
    }

    companion object {
        val registeredTasks = mutableSetOf<PeriodicTaskContext>()
    }

    suspend fun execute() {
        while (true) {
            delay(20.seconds)

            val now = LocalDateTime.now(clock)
            println("Finding executable tasks at $now")

            registeredTasks
                .also { println("$it") }
                .filter { it.nextExecution <= now && it.status == WAITING }.toSet()
                .also { registeredTasks.removeAll(it) }
                .map { it.copy(status = TaskStatus.EXECUTING) }
                .also { registeredTasks.addAll(it) }
                .onEach {
                    println("Launching task ${it.id}")
                    GlobalScope.launch { it.execute(now) }
                }
        }
    }

    suspend fun register(definition: PeriodicTaskDefinition) {
        val ctx = PeriodicTaskContext(
            id = registeredTasks.size + 1,
            definition = definition,
            status = WAITING,
            nextExecution = LocalDateTime.now(clock).nextBy(definition.cron),
            lastExecution = null,
        )
        registeredTasks.add(ctx)
    }

    suspend fun PeriodicTaskContext.execute(startedAt: LocalDateTime) {
        println("Starting task $id")
        delay(30.seconds)
        println("  task $id: ${definition.message}")
        finish(startedAt)
    }

    suspend fun PeriodicTaskContext.finish(startedAt: LocalDateTime) {
        registeredTasks.apply {
            removeIf { it.id == id }
            add(
                copy(
                    lastExecution = startedAt,
                    nextExecution = startedAt.nextBy(definition.cron),
                    status = WAITING,
                ),
            )
        }
    }
}

private fun LocalDateTime.nextBy(cron: CronExpression) = cron.next(toJavaLocalDateTime())!!.toKotlinLocalDateTime()
