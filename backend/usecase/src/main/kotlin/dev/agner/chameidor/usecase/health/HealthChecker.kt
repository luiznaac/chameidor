package dev.agner.chameidor.usecase.health

interface HealthChecker {

    suspend fun getHealthStatus(): HealthCheckResult
}
