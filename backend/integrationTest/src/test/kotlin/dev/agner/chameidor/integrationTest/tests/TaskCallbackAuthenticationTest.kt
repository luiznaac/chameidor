package dev.agner.chameidor.integrationTest.tests

import com.github.tomakehurst.wiremock.http.RequestMethod
import dev.agner.chameidor.httpapi.controller.TaskExecutionResponse
import dev.agner.chameidor.httpapi.controller.TaskRegistrationResponse
import dev.agner.chameidor.httpapi.controller.TaskResponse
import dev.agner.chameidor.integrationTest.config.HttpMockService
import dev.agner.chameidor.integrationTest.config.IntegrationTest
import dev.agner.chameidor.integrationTest.helpers.getBean
import dev.agner.chameidor.persistence.auth.ExternalSystemTable
import dev.agner.chameidor.usecase.auth.TokenHasher
import dev.agner.chameidor.usecase.task.EXTERNAL_SYSTEM_HEADER
import dev.agner.chameidor.usecase.task.TASK_ID_HEADER
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// End-to-end: a task registered by valoab is executed by chameidor, which calls the consumer's
// endpoint. The consumer here is WireMock acting like a receiver that authenticates callers — it
// only answers the callback when chameidor presents its own registered identity token, plus the
// creator and task-id headers (contracts/README.md, docs/external-systems.md).
@IntegrationTest
class TaskCallbackAuthenticationTest : StringSpec({

    val systemName = "valoab"
    val systemToken = "dev-valoab-fixture-token"
    val chameidorToken = "dev-chameidor-fixture-token"
    val callbackEndpoint = "/callback"

    fun registerSystem() {
        transaction {
            ExternalSystemTable.insert {
                it[id] = systemName
                it[tokenHash] = TokenHasher.sha256(systemToken)
                it[ExternalSystemTable.active] = true
            }
        }
    }

    suspend fun registerTask(): Int {
        val response = getBean<HttpClient>().post("http://localhost:8080/tasks/one-time") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $systemToken")
            setBody(
                mapOf(
                    "host" to "localhost:3000",
                    "endpoint" to callbackEndpoint,
                    "data" to mapOf("product_id" to 1),
                ),
            )
        }
        response.status shouldBe HttpStatusCode.Created
        return response.body<TaskRegistrationResponse>().id
    }

    suspend fun awaitTask(taskId: Int): TaskResponse {
        repeat(60) {
            val task = getBean<HttpClient>().get("http://localhost:8080/tasks/$taskId").body<TaskResponse>()
            if (task.status == "EXECUTED") return task
            delay(500)
        }
        error("task $taskId was not executed in time")
    }

    suspend fun singleExecution(taskId: Int): TaskExecutionResponse =
        getBean<HttpClient>().get("http://localhost:8080/tasks/$taskId/executions")
            .body<List<TaskExecutionResponse>>()
            .single()

    "a consumer that validates chameidor's bearer accepts the callback end to end" {
        registerSystem()
        HttpMockService.configureResponses {
            response {
                method = RequestMethod.POST
                endpoint = callbackEndpoint
                headers = mapOf(
                    HttpHeaders.Authorization to "Bearer $chameidorToken",
                    EXTERNAL_SYSTEM_HEADER to systemName,
                )
                payload = mapOf("ok" to true)
            }
        }

        val taskId = registerTask()
        awaitTask(taskId)

        singleExecution(taskId).status shouldBe "SUCCESS"
        HttpMockService.verify(
            method = RequestMethod.POST,
            endpoint = callbackEndpoint,
            headers = mapOf(
                HttpHeaders.Authorization to "Bearer $chameidorToken",
                EXTERNAL_SYSTEM_HEADER to systemName,
                TASK_ID_HEADER to taskId.toString(),
            ),
        )
    }

    "a consumer that refuses chameidor's bearer makes the run fail, not silently succeed" {
        registerSystem()
        HttpMockService.configureResponses {
            response {
                method = RequestMethod.POST
                endpoint = callbackEndpoint
                headers = mapOf(HttpHeaders.Authorization to "Bearer $chameidorToken")
                httpStatus = HttpStatusCode.Unauthorized.value
                payload = mapOf("error" to "invalid bearer credential")
            }
        }

        val taskId = registerTask()
        awaitTask(taskId)

        singleExecution(taskId).status shouldBe "FAILURE"
    }
})
