package dev.agner.chameidor.usecase.task

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

private const val JOB_ID = "job-12345678"
private const val TASK_ID = 7

class TaskExecutorTest : StringSpec({

    lateinit var logger: Logger
    lateinit var events: ListAppender<ILoggingEvent>

    beforeTest {
        val context = LoggerFactory.getILoggerFactory() as LoggerContext
        logger = context.getLogger(TaskExecutor::class.java)
        events = ListAppender()
        events.context = context
        events.start()
        logger.addAppender(events)
    }

    afterTest {
        logger.detachAppender(events)
    }

    "a failed execution logs one ERROR carrying the job id and task id" {
        val callClient = mockk<CallClient> {
            coEvery { makeCall(any()) } throws IllegalStateException("boom")
        }
        val executor = TaskExecutor(clock(), callClient, mockk(relaxed = true), mockk(relaxed = true))

        runTest {
            executor.execute(task(), JOB_ID)
        }

        val event = events.list.single()
        event.level shouldBe Level.ERROR
        event.message shouldBe "tasks.execution.failed"
        event.throwableProxy.shouldNotBeNull().className shouldBe "java.lang.IllegalStateException"
        event.mdcPropertyMap[TaskTelemetryAttributes.JOB_ID] shouldBe JOB_ID
        event.mdcPropertyMap[TaskTelemetryAttributes.TASK_ID] shouldBe TASK_ID.toString()
        event.mdcPropertyMap["error.type"] shouldBe "IllegalStateException"
        event.mdcPropertyMap.containsKey("request.id") shouldBe false
    }

    "a successful execution emits no record" {
        val callClient = mockk<CallClient> {
            coEvery { makeCall(any()) } returns "ok"
        }
        val executionRepository = mockk<ITaskExecutionRepository>(relaxed = true)
        val executor = TaskExecutor(clock(), callClient, mockk(relaxed = true), executionRepository)

        runTest {
            executor.execute(task(), JOB_ID)
        }

        events.list.shouldBeEmpty()
        coVerify { executionRepository.save(TASK_ID, any(), any()) }
    }
})

private fun clock(): Clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

private fun task() = Task.OneTimeTask(
    id = TASK_ID,
    host = "http://host",
    endpoint = "/callback",
    data = null,
    createdBy = "sys",
)
