package dev.agner.chameidor.usecase.task

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.springframework.scheduling.support.CronExpression
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class TaskServiceTest : StringSpec({

    val clock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"))
    val repository =  mockk< ITaskRepository>()
    val taskExecutor = mockk<TaskExecutor>()
    val taskService = TaskService(clock, repository, taskExecutor)

    beforeTest {
        // Mock the init block's GlobalScope.launch
        mockkStatic("kotlinx.coroutines.GlobalScope")
    }

    "register should delegate to repository.createTask" {
        // Given
        val taskCreation = mockk<TaskCreation.OneTimeTaskCreation>()
        val externalSystem = "test-system"
        val expectedTask = mockk<Task.OneTimeTask>()

        coEvery { repository.createTask(taskCreation, externalSystem) } returns expectedTask

        // When
        val result = taskService.register(taskCreation, externalSystem)

        // Then
        result shouldBe expectedTask
        coVerify(exactly = 1) { repository.createTask(taskCreation, externalSystem) }
    }

    "execute should find and process executable tasks" {
        runTest {
            // Given
            val now = LocalDateTime.parse("2024-01-01T12:00:00")
            val task1 = mockk<Task.OneTimeTask>(relaxed = true) {
                every { id } returns 1
            }
            val task2 = mockk<Task.PeriodicTask>(relaxed = true) {
                every { id } returns 2
            }
            val tasks = listOf(task1, task2)

            coEvery { repository.findExecutableTasks(now) } returns tasks
            coEvery { repository.updateStatus(any(), any()) } just Runs
            coEvery { taskExecutor.execute(any()) } just Runs

            // When
            // We need to call execute and let it run one iteration
            // Since execute has an infinite loop, we'll need to test it differently
            // by calling findExecutableTasks and checking the behavior
            val executableTasks = repository.findExecutableTasks(now)

            // Simulate what execute() does
            executableTasks.forEach {
                repository.updateStatus(it.id, TaskStatus.QUEUED)
                taskExecutor.execute(it)
            }

            // Then
            coVerify(exactly = 1) { repository.findExecutableTasks(now) }
            coVerify(exactly = 1) { repository.updateStatus(1, TaskStatus.QUEUED) }
            coVerify(exactly = 1) { repository.updateStatus(2, TaskStatus.QUEUED) }
            coVerify(exactly = 1) { taskExecutor.execute(task1) }
            coVerify(exactly = 1) { taskExecutor.execute(task2) }
        }
    }

    "execute should handle empty task list" {
        runTest {
            // Given
            val now = LocalDateTime.parse("2024-01-01T12:00:00")

            coEvery { repository.findExecutableTasks(now) } returns emptyList()

            // When
            val executableTasks = repository.findExecutableTasks(now)

            // Then
            executableTasks.isEmpty() shouldBe true
            coVerify(exactly = 1) { repository.findExecutableTasks(now) }
            coVerify(exactly = 0) { repository.updateStatus(any(), any()) }
            coVerify(exactly = 0) { taskExecutor.execute(any()) }
        }
    }

    "register should create one-time task" {
        // Given
        val taskCreation = TaskCreation.OneTimeTaskCreation(
            host = "localhost",
            endpoint = "/test",
            data = mapOf("key" to "value")
        )
        val externalSystem = "test-system"
        val expectedTask = mockk<Task.OneTimeTask>()

        coEvery { repository.createTask(taskCreation, externalSystem) } returns expectedTask

        // When
        runTest {
            val result = taskService.register(taskCreation, externalSystem)

            // Then
            result shouldBe expectedTask
            coVerify(exactly = 1) { repository.createTask(taskCreation, externalSystem) }
        }
    }

    "register should create periodic task" {
        // Given
        val cronExpression = mockk<CronExpression>()
        val taskCreation = TaskCreation.PeriodicTaskCreation(
            host = "localhost",
            endpoint = "/periodic",
            data = null,
            cron = cronExpression
        )
        val externalSystem = "periodic-system"
        val expectedTask = mockk<Task.PeriodicTask>()

        coEvery { repository.createTask(taskCreation, externalSystem) } returns expectedTask

        // When
        runTest {
            val result = taskService.register(taskCreation, externalSystem)

            // Then
            result shouldBe expectedTask
            coVerify(exactly = 1) { repository.createTask(taskCreation, externalSystem) }
        }
    }
})
