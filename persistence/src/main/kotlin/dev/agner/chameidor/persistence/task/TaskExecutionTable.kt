package dev.agner.chameidor.persistence.task

import com.fasterxml.jackson.module.kotlin.readValue
import dev.agner.chameidor.usecase.configuration.JsonMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.json.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val executionMapper = JsonMapper.jsonAdapter()

object TaskExecutionTable : IntIdTable("task_execution") {
    val taskId = reference("task_id", TaskTable)
    val executedAt = datetime("executed_at")
    val durationMs = long("duration_ms")
    val status = varchar("status", 20)
    val result = json(
        "result",
        { executionMapper.writeValueAsString(it) },
        { executionMapper.readValue<Any>(it) },
    ).nullable()
    val createdAt = datetime("created_at")
}

class TaskExecutionEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskExecutionEntity>(TaskExecutionTable)

    var taskId by TaskExecutionTable.taskId
    var executedAt by TaskExecutionTable.executedAt
    var durationMs by TaskExecutionTable.durationMs
    var status by TaskExecutionTable.status
    var result by TaskExecutionTable.result
    var createdAt by TaskExecutionTable.createdAt

    var duration: Duration
        get() = durationMs.milliseconds
        set(value) {
            durationMs = value.inWholeMilliseconds
        }
}
