package dev.agner.chameidor.usecase.auth

interface IExternalSystemRepository {

    suspend fun findByTokenHash(tokenHash: String): ExternalSystem?

    suspend fun findByName(name: String): ExternalSystem?
}
