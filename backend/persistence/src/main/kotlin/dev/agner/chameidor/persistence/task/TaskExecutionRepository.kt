package dev.agner.chameidor.persistence.task

import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.ITaskExecutionRepository
import dev.agner.chameidor.usecase.task.TaskExecutionResult
import dev.agner.chameidor.usecase.task.TaskExecutionView
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class TaskExecutionRepository(private val clock: Clock) : ITaskExecutionRepository {

    override suspend fun save(taskId: Int, executedAt: LocalDateTime, taskExecutionResult: TaskExecutionResult) {
        transaction {
            TaskExecutionEntity.new {
                this.taskId = EntityID(taskId, TaskTable)
                this.executedAt = executedAt
                duration = taskExecutionResult.duration

                when (taskExecutionResult) {
                    is TaskExecutionResult.Success -> {
                        status = "SUCCESS"
                        result = taskExecutionResult.response
                    }
                    is TaskExecutionResult.Failure -> {
                        status = "FAILURE"
                        result = mapOf("message" to taskExecutionResult.message)
                    }
                }

                createdAt = LocalDateTime.now(clock)
            }
        }
    }

    override suspend fun findByTaskId(taskId: Int, limit: Int, offset: Int): List<TaskExecutionView> = transaction {
        TaskExecutionEntity.find { TaskExecutionTable.taskId eq taskId }
            .orderBy(TaskExecutionTable.executedAt to SortOrder.DESC)
            .toList()
            .drop(offset)
            .take(limit)
            .map { it.toView() }
    }

    override suspend fun findRecent(limit: Int): List<TaskExecutionView> = transaction {
        TaskExecutionEntity.all()
            .orderBy(TaskExecutionTable.executedAt to SortOrder.DESC)
            .toList()
            .take(limit)
            .map { it.toView() }
    }

    private fun TaskExecutionEntity.toView() = TaskExecutionView(
        id = id.value,
        taskId = taskId.value,
        executedAt = executedAt,
        durationMs = durationMs,
        status = status,
        result = result,
    )
}
