package dev.agner.chameidor.persistence.task

import com.fasterxml.jackson.module.kotlin.readValue
import dev.agner.chameidor.usecase.configuration.JsonMapper
import dev.agner.chameidor.usecase.task.Task.OneTimeTask
import dev.agner.chameidor.usecase.task.Task.PeriodicTask
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.json.json
import org.springframework.scheduling.support.CronExpression
import kotlin.time.ExperimentalTime

private val mapper = JsonMapper.jsonAdapter()

@OptIn(ExperimentalTime::class)
object TaskTable : IntIdTable("task") {
    val host = varchar("host", 50)
    val endpoint = varchar("endpoint", 255)
    val data = json("data", { mapper.writeValueAsString(it) }, { mapper.readValue<Any>(it) }).nullable()
    val cron = varchar("cron", 20).nullable()
    val status = varchar("status", 30)
    val executedAt = datetime("executed_at").nullable()
    val nextExecutionAt = datetime("next_execution_at").nullable()
    val createdBy = varchar("created_by", 50)
    val createdAt = datetime("created_at")
}

@OptIn(ExperimentalTime::class)
class TaskEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TaskEntity>(TaskTable)

    var host by TaskTable.host
    var endpoint by TaskTable.endpoint
    var data by TaskTable.data
    var cron by TaskTable.cron
    var status by TaskTable.status
    var executedAt by TaskTable.executedAt
    var nextExecutionAt by TaskTable.nextExecutionAt
    var createdBy by TaskTable.createdBy
    var createdAt by TaskTable.createdAt

    fun toModel() = when {
        cron != null -> PeriodicTask(id.value, host, endpoint, data, CronExpression.parse("0 " + cron!!))
        else -> OneTimeTask(id.value, host, endpoint, data)
    }
}
