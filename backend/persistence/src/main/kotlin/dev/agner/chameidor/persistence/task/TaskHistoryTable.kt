package dev.agner.chameidor.persistence.task

import com.fasterxml.jackson.module.kotlin.readValue
import dev.agner.chameidor.usecase.configuration.JsonMapper
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.json.json
import kotlin.time.ExperimentalTime

private val mapper = JsonMapper.jsonAdapter()

@OptIn(ExperimentalTime::class)
object TaskHistoryTable : IntIdTable("task_history") {
    val taskId = integer("task_id")
    val host = varchar("host", 50)
    val endpoint = varchar("endpoint", 255)
    val data = json("data", { mapper.writeValueAsString(it) }, { mapper.readValue<Any>(it) }).nullable()
    val cron = varchar("cron", 20).nullable()
    val status = varchar("status", 30)
    val executedAt = datetime("executed_at").nullable()
    val nextExecutionAt = datetime("next_execution_at").nullable()
    val createdAt = datetime("created_at")
}

@OptIn(ExperimentalTime::class)
class TaskHistoryEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskHistoryEntity>(TaskHistoryTable)

    var taskId by TaskHistoryTable.taskId
    var host by TaskHistoryTable.host
    var endpoint by TaskHistoryTable.endpoint
    var data by TaskHistoryTable.data
    var cron by TaskHistoryTable.cron
    var status by TaskHistoryTable.status
    var executedAt by TaskHistoryTable.executedAt
    var nextExecutionAt by TaskHistoryTable.nextExecutionAt
    var createdAt by TaskHistoryTable.createdAt
}
