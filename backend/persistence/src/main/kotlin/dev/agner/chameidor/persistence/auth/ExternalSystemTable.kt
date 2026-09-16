package dev.agner.chameidor.persistence.auth

import dev.agner.chameidor.usecase.auth.ExternalSystem
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

object ExternalSystemTable : IdTable<String>("external_systems") {
    override val id = varchar("name", 50).entityId()
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val active = bool("active")

    override val primaryKey = PrimaryKey(id)
}

class ExternalSystemEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, ExternalSystemEntity>(ExternalSystemTable)

    var active by ExternalSystemTable.active

    fun toModel() = ExternalSystem(name = id.value, active = active)
}
