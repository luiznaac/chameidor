package dev.agner.chameidor.usecase.task

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import dev.agner.chameidor.usecase.commons.RequestContext
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TaskServiceTest : StringSpec({

    lateinit var logger: Logger
    lateinit var events: ListAppender<ILoggingEvent>

    beforeTest {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        logger = context.getLogger(TaskService::class.java)
        events = ListAppender()
        events.context = context
        events.start()
        logger.addAppender(events)
    }

    afterTest {
        logger.detachAppender(events)
    }

    "mints a fresh job id at the enqueue point and hands it to the executor" {
        val repository = mockk<ITaskRepository>()
        val executor = mockk<TaskExecutor>()
        val tasks = mutableListOf<Task>()
        val jobIds = mutableListOf<String>()
        coEvery { repository.findExecutableTasks(any()) } returns listOf(task())
        coEvery { repository.updateStatus(any(), any()) } just Runs
        coEvery { executor.execute(capture(tasks), capture(jobIds)) } just Runs

        val service = TaskService(clock(), repository, executor, 10)
        try {
            runBlocking {
                service.enqueueTasks()
                withTimeout(5.seconds) {
                    while (jobIds.isEmpty()) {
                        delay(10.milliseconds)
                    }
                }
            }

            tasks.shouldHaveSize(1).first().id shouldBe 7
            jobIds.single() shouldMatch RequestContext.GENERATED_REQUEST_ID
        } finally {
            service.destroy()
        }
    }

    "a scheduler failure is one ERROR carrying the scheduler job id" {
        val repository = mockk<ITaskRepository>()
        coEvery { repository.findExecutableTasks(any()) } throws IllegalStateException("db down")

        val service = TaskService(clock(), repository, mockk(relaxed = true), 10)
        try {
            runBlocking {
                service.enqueueOnce()
            }

            val event = events.list.single()
            event.level shouldBe Level.ERROR
            event.message shouldBe "tasks.enqueue.failed"
            event.throwableProxy.shouldNotBeNull().className shouldBe "java.lang.IllegalStateException"
            event.mdcPropertyMap[TaskTelemetryAttributes.JOB_ID] shouldMatch RequestContext.GENERATED_REQUEST_ID
            event.mdcPropertyMap["error.type"] shouldBe "IllegalStateException"
        } finally {
            service.destroy()
        }
    }

    "enqueues nothing when no task is due" {
        val repository = mockk<ITaskRepository>()
        val executor = mockk<TaskExecutor>()
        val jobIds = mutableListOf<String>()
        coEvery { repository.findExecutableTasks(any()) } returns emptyList()
        coEvery { executor.execute(any(), capture(jobIds)) } just Runs

        val service = TaskService(clock(), repository, executor, 10)
        try {
            runBlocking {
                service.enqueueTasks()
                delay(200)
            }

            jobIds.shouldBeEmpty()
        } finally {
            service.destroy()
        }
    }
})

private fun clock(): Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

private fun task() = Task.OneTimeTask(
    id = 7,
    host = "http://host",
    endpoint = "/callback",
    data = null,
    createdBy = "sys",
)
