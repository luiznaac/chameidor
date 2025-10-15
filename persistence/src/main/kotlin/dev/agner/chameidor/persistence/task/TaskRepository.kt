package dev.agner.chameidor.persistence.task

import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.task.ITaskRepository
import dev.agner.chameidor.usecase.task.Task
import dev.agner.chameidor.usecase.task.TaskCreation
import dev.agner.chameidor.usecase.task.TaskStatus
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class TaskRepository(private val clock: Clock) : ITaskRepository {

    override suspend fun createTask(task: TaskCreation, createdBy: String): Task = transaction {
        val now = LocalDateTime.now(clock)
        TaskEntity.new {
            host = task.host
            endpoint = task.endpoint
            data = task.data
            cron = when (task) {
                is TaskCreation.PeriodicTaskCreation -> task.cron.toString().removePrefix("0 ")
                is TaskCreation.OneTimeTaskCreation -> null
            }
            status = TaskStatus.WAITING.name
            nextExecutionAt = now
            this.createdBy = createdBy
            createdAt = now
        }
            .also { saveToHistory(it) }
            .toModel()
    }

    override suspend fun findExecutableTasks(now: LocalDateTime): List<Task> = transaction {
        TaskEntity.find {
            (TaskTable.status eq TaskStatus.WAITING.name) and
                (TaskTable.nextExecutionAt lessEq now)
        }
            .map { it.toModel() }
    }

    override suspend fun updateStatus(id: Int, status: TaskStatus): Unit = transaction {
        TaskEntity.findByIdAndUpdate(id) {
            it.status = status.name
        }.also { saveToHistory(it!!) }
    }

    override suspend fun updateExecutedAt(id: Int, executedAt: LocalDateTime): Unit = transaction {
        TaskEntity.findByIdAndUpdate(id) {
            it.executedAt = executedAt
            it.status = TaskStatus.EXECUTED.name
        }.also { saveToHistory(it!!) }
    }

    override suspend fun updateNextExecution(id: Int, nextExecutionAt: LocalDateTime): Unit = transaction {
        TaskEntity.findByIdAndUpdate(id) {
            it.nextExecutionAt = nextExecutionAt
            it.status = TaskStatus.WAITING.name
        }.also { saveToHistory(it!!) }
    }

    private fun saveToHistory(taskEntity: TaskEntity) {
        TaskHistoryEntity.new {
            taskId = taskEntity.id.value
            host = taskEntity.host
            endpoint = taskEntity.endpoint
            data = taskEntity.data
            cron = taskEntity.cron
            status = taskEntity.status
            executedAt = taskEntity.executedAt
            nextExecutionAt = taskEntity.nextExecutionAt
            createdAt = LocalDateTime.now(clock)
        }
    }
}
