package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.task.TaskExecutionView
import dev.agner.chameidor.usecase.task.TaskHistoryEntry
import dev.agner.chameidor.usecase.task.TaskStatus
import dev.agner.chameidor.usecase.task.TaskView
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import org.springframework.scheduling.support.CronExpression

class TaskResponseTest : StringSpec({

    val now = LocalDateTime(2026, 1, 2, 3, 4, 5)

    fun view(cron: CronExpression?) = TaskView(
        id = 1,
        host = "http://host",
        endpoint = "/e",
        data = mapOf("k" to "v"),
        cron = cron,
        status = TaskStatus.EXECUTED,
        createdBy = "sys",
        createdAt = now,
        executedAt = now,
        nextExecutionAt = null,
    )

    "maps a one-time task to type one_time with a null cron" {
        val response = TaskResponse.from(view(null))

        response.type shouldBe "one_time"
        response.cron shouldBe null
        response.status shouldBe "EXECUTED"
    }

    "maps a periodic task to type periodic with the 5-field cron string" {
        val response = TaskResponse.from(view(CronExpression.parse("0 */5 * * * *")))

        response.type shouldBe "periodic"
        response.cron shouldBe "*/5 * * * *"
    }

    "maps an execution view straight through" {
        val response = TaskExecutionResponse.from(TaskExecutionView(3, 1, now, 42L, "FAILURE", null))

        response.taskId shouldBe 1
        response.durationMs shouldBe 42L
        response.status shouldBe "FAILURE"
    }

    "maps a history entry straight through" {
        val response = TaskHistoryResponse.from(TaskHistoryEntry("QUEUED", null, now, now))

        response.status shouldBe "QUEUED"
        response.nextExecutionAt shouldBe now
    }
})
