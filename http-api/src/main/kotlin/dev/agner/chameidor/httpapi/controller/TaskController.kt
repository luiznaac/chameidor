package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.task.TaskCreation.OneTimeTaskCreation
import dev.agner.chameidor.usecase.task.TaskCreation.PeriodicTaskCreation
import dev.agner.chameidor.usecase.task.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.springframework.stereotype.Component

@Component
class TaskController(
    private val periodicTaskService: TaskService,
) : ControllerTemplate {

    override fun routes(): RouteDefinition = {
        route("/tasks") {
            post("/periodic") {
                val payload = call.receive<PeriodicTaskCreation>()
                val externalSystem = call.request.headers["X-External-System"]
                    ?: throw IllegalArgumentException("X-External-System header is required")

                call.respond(
                    HttpStatusCode.Created,
                    periodicTaskService.register(payload, externalSystem),
                )
            }

            post("/one-time") {
                val payload = call.receive<OneTimeTaskCreation>()
                val externalSystem = call.request.headers["X-External-System"]
                    ?: throw IllegalArgumentException("X-External-System header is required")

                call.respond(
                    HttpStatusCode.Created,
                    periodicTaskService.register(payload, externalSystem),
                )
            }
        }
    }
}
