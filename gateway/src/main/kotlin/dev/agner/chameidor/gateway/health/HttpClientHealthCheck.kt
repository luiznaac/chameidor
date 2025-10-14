package dev.agner.chameidor.gateway.health

import dev.agner.chameidor.usecase.health.HealthCheckResult
import dev.agner.chameidor.usecase.health.HealthChecker
import dev.agner.chameidor.usecase.health.HealthGateway
import org.springframework.stereotype.Service

@Service
class HttpClientHealthCheck(
    private val healthGateway: HealthGateway,
) : HealthChecker {

    override suspend fun getHealthStatus() = HealthCheckResult(
        serviceName = "http-client",
        isHealthy = healthGateway.isHealthy(),
    )
}
