package dev.agner.chameidor.usecase.health

interface HealthGateway {
    suspend fun isHealthy(): Boolean
}
