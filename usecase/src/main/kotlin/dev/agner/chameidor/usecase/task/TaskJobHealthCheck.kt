package dev.agner.chameidor.usecase.task

import dev.agner.chameidor.usecase.commons.now
import dev.agner.chameidor.usecase.health.HealthCheckResult
import dev.agner.chameidor.usecase.health.HealthChecker
import kotlinx.datetime.LocalDateTime
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class TaskJobHealthCheck(
    private val taskService: TaskService,
    private val clock: Clock,
) : HealthChecker {
    override suspend fun getHealthStatus() =
        HealthCheckResult(
            serviceName = "task-job",
            isHealthy = try {
                taskService.isRunning()
            } catch (e: Exception) {
                false
            },
            timestamp = LocalDateTime.now(clock),
        )
}
