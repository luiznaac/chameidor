package dev.agner.chameidor.persistence.auth

import dev.agner.chameidor.usecase.auth.ExternalSystem
import dev.agner.chameidor.usecase.auth.IExternalSystemRepository
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.stereotype.Component

@Component
class ExternalSystemRepository : IExternalSystemRepository {

    override suspend fun findByTokenHash(tokenHash: String): ExternalSystem? = transaction {
        ExternalSystemEntity.find { ExternalSystemTable.tokenHash eq tokenHash }
            .firstOrNull()
            ?.toModel()
    }

    override suspend fun findByName(name: String): ExternalSystem? = transaction {
        ExternalSystemEntity.findById(name)?.toModel()
    }
}
