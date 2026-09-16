package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.auth.AuthorizationResult
import dev.agner.chameidor.usecase.auth.AuthorizationService
import dev.agner.chameidor.usecase.auth.ExternalSystem
import dev.agner.chameidor.usecase.task.TaskCreation.OneTimeTaskCreation
import dev.agner.chameidor.usecase.task.TaskCreation.PeriodicTaskCreation
import dev.agner.chameidor.usecase.task.TaskFilter
import dev.agner.chameidor.usecase.task.TaskQueryService
import dev.agner.chameidor.usecase.task.TaskService
import dev.agner.chameidor.usecase.task.TaskStatus
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.springframework.stereotype.Component

private const val DEFAULT_EXECUTIONS_LIMIT = 20
private const val EXTERNAL_SYSTEM_HEADER = "X-External-System"

@Component
class TaskController(
    private val taskService: TaskService,
    private val taskQueryService: TaskQueryService,
    private val authorizationService: AuthorizationService,
) : ControllerTemplate {

    override fun routes(): RouteDefinition = {
        route("/tasks") {
            post("/periodic") {
                call.authorizedSystem()?.let { system ->
                    val payload = call.receive<PeriodicTaskCreation>()
                    call.respond(HttpStatusCode.Created, taskService.register(payload, system.name))
                }
            }

            post("/one-time") {
                call.authorizedSystem()?.let { system ->
                    val payload = call.receive<OneTimeTaskCreation>()
                    call.respond(HttpStatusCode.Created, taskService.register(payload, system.name))
                }
            }

            get {
                val filter = TaskFilter(
                    status = call.request.queryParameters["status"]?.let { TaskStatus.valueOf(it.uppercase()) },
                    createdBy = call.request.queryParameters["created_by"],
                    limit = call.request.queryParameters["limit"]?.toInt() ?: TaskFilter.DEFAULT_LIMIT,
                    offset = call.request.queryParameters["offset"]?.toInt() ?: 0,
                )
                call.respond(taskQueryService.list(filter).map(TaskResponse::from))
            }

            // Must precede "/{id}" so it is not captured as an id.
            get("/executions") {
                val limit = call.request.queryParameters["limit"]?.toInt() ?: DEFAULT_EXECUTIONS_LIMIT
                call.respond(taskQueryService.recentExecutions(limit).map(TaskExecutionResponse::from))
            }

            get("/{id}") {
                val view = taskQueryService.get(call.taskId())
                if (view == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("task not found"))
                } else {
                    call.respond(TaskResponse.from(view))
                }
            }

            get("/{id}/executions") {
                val limit = call.request.queryParameters["limit"]?.toInt() ?: DEFAULT_EXECUTIONS_LIMIT
                val offset = call.request.queryParameters["offset"]?.toInt() ?: 0
                call.respond(
                    taskQueryService.executions(call.taskId(), limit, offset).map(TaskExecutionResponse::from),
                )
            }

            get("/{id}/history") {
                call.respond(taskQueryService.history(call.taskId()).map(TaskHistoryResponse::from))
            }
        }
    }

    private suspend fun ApplicationCall.authorizedSystem(): ExternalSystem? {
        val result = authorizationService.authorize(
            caller = request.headers[EXTERNAL_SYSTEM_HEADER],
            target = request.path(),
            credential = bearerCredential(),
        )

        return when (result) {
            is AuthorizationResult.Authorized -> result.system
            is AuthorizationResult.Unauthorized -> {
                respond(HttpStatusCode.Unauthorized, ErrorResponse(result.reason))
                null
            }
            is AuthorizationResult.Forbidden -> {
                respond(HttpStatusCode.Forbidden, ErrorResponse(result.reason))
                null
            }
        }
    }

    private fun ApplicationCall.bearerCredential(): String? =
        request.headers[HttpHeaders.Authorization]
            ?.split(" ", limit = 2)
            ?.takeIf { it.size == 2 && it[0].equals("Bearer", ignoreCase = true) }
            ?.get(1)
            ?.trim()
            ?.takeIf { it.isNotBlank() && it.none(Char::isWhitespace) }
}

private fun ApplicationCall.taskId() =
    parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("task id must be an integer")
