package dev.agner.chameidor.persistence.task

import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.ITaskExecutionRepository
import dev.agner.chameidor.usecase.task.TaskExecutionResult
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.EntityID
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
                        result = mapOf(
                            "message" to taskExecutionResult.exception.message,
                            "type" to taskExecutionResult.exception::class.qualifiedName,
                            "stackTrace" to taskExecutionResult.exception.stackTraceToString(),
                        )
                    }
                }

                createdAt = LocalDateTime.now(clock)
            }
        }
    }
}
