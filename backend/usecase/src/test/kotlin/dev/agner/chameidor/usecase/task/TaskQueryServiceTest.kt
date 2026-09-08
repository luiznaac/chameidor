package dev.agner.chameidor.usecase.task

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime

class TaskQueryServiceTest : StringSpec({

    val repository = mockk<ITaskRepository>()
    val executionRepository = mockk<ITaskExecutionRepository>()
    val service = TaskQueryService(repository, executionRepository)

    val now = LocalDateTime(2026, 1, 2, 3, 4, 5)

    fun view(id: Int) = TaskView(
        id = id,
        host = "http://host",
        endpoint = "/e",
        data = null,
        cron = null,
        status = TaskStatus.WAITING,
        createdBy = "sys",
        createdAt = now,
        executedAt = null,
        nextExecutionAt = now,
    )

    "list delegates to the repository with the given filter" {
        runTest {
            val filter = TaskFilter(status = TaskStatus.QUEUED, createdBy = "sys")
            coEvery { repository.findAll(filter) } returns listOf(view(1), view(2))

            service.list(filter).map { it.id } shouldBe listOf(1, 2)
        }
    }

    "get returns null when the task is not found" {
        runTest {
            coEvery { repository.findViewById(9) } returns null

            service.get(9) shouldBe null
        }
    }

    "executions delegates with pagination" {
        runTest {
            val exec = TaskExecutionView(1, 7, now, 12L, "SUCCESS", null)
            coEvery { executionRepository.findByTaskId(7, 10, 5) } returns listOf(exec)

            service.executions(7, 10, 5) shouldBe listOf(exec)
        }
    }

    "recentExecutions delegates with the given limit" {
        runTest {
            coEvery { executionRepository.findRecent(3) } returns emptyList()

            service.recentExecutions(3) shouldBe emptyList()
        }
    }

    "history delegates to the repository" {
        runTest {
            val entry = TaskHistoryEntry("WAITING", null, now, now)
            coEvery { repository.findHistory(4) } returns listOf(entry)

            service.history(4) shouldBe listOf(entry)
        }
    }
})
