package dev.agner.chameidor.usecase.task

import org.springframework.stereotype.Service

/**
 * Read side of the task aggregate. [TaskService] owns the enqueue loop and
 * registration; this class only answers queries for the HTTP layer.
 */
@Service
class TaskQueryService(
    private val repository: ITaskRepository,
    private val executionRepository: ITaskExecutionRepository,
) {

    suspend fun list(filter: TaskFilter): List<TaskView> = repository.findAll(filter)

    suspend fun get(id: Int): TaskView? = repository.findViewById(id)

    suspend fun executions(taskId: Int, limit: Int, offset: Int): List<TaskExecutionView> =
        executionRepository.findByTaskId(taskId, limit, offset)

    suspend fun recentExecutions(limit: Int): List<TaskExecutionView> =
        executionRepository.findRecent(limit)

    suspend fun history(taskId: Int): List<TaskHistoryEntry> = repository.findHistory(taskId)
}
