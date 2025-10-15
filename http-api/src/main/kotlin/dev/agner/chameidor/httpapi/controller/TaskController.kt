package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.task.PeriodicTaskService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.netty.handler.codec.http.HttpResponseStatus.CREATED
import org.springframework.stereotype.Component

@Component
class TaskController(
    private val periodicTaskService: PeriodicTaskService,
) : ControllerTemplate {

    override fun routes(): RouteDefinition = {
        route("/tasks") {
            post("/periodic") {
                periodicTaskService.register(call.receive())
                call.respond(CREATED)
            }
        }
    }
}
