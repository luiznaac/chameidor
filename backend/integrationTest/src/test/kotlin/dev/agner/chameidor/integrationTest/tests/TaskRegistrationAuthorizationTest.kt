package dev.agner.chameidor.integrationTest.tests

import dev.agner.chameidor.httpapi.controller.ErrorResponse
import dev.agner.chameidor.httpapi.controller.TaskResponse
import dev.agner.chameidor.integrationTest.config.IntegrationTest
import dev.agner.chameidor.integrationTest.helpers.getBean
import dev.agner.chameidor.persistence.auth.ExternalSystemTable
import dev.agner.chameidor.usecase.auth.AuthorizationResult
import dev.agner.chameidor.usecase.auth.TokenHasher
import dev.agner.chameidor.usecase.task.EXTERNAL_SYSTEM_HEADER
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

@IntegrationTest
class TaskRegistrationAuthorizationTest : StringSpec({

    val systemName = "valoab"
    val token = "dev-valoab-fixture-token"
    val rotatedToken = "dev-valoab-rotated-fixture-token"

    fun register(
        name: String = systemName,
        credential: String = token,
        active: Boolean = true,
    ) {
        transaction {
            ExternalSystemTable.insert {
                it[id] = name
                it[tokenHash] = TokenHasher.sha256(credential)
                it[ExternalSystemTable.active] = active
            }
        }
    }

    fun setCredential(credential: String) {
        transaction {
            ExternalSystemTable.update({ ExternalSystemTable.id eq systemName }) {
                it[tokenHash] = TokenHasher.sha256(credential)
            }
        }
    }

    fun deactivate() {
        transaction {
            ExternalSystemTable.update({ ExternalSystemTable.id eq systemName }) {
                it[ExternalSystemTable.active] = false
            }
        }
    }

    suspend fun registerOneTime(authorization: String? = null, caller: String? = null): HttpResponse =
        getBean<HttpClient>().post("http://localhost:8080/tasks/one-time") {
            contentType(ContentType.Application.Json)
            authorization?.let { header(HttpHeaders.Authorization, it) }
            caller?.let { header(EXTERNAL_SYSTEM_HEADER, it) }
            setBody(mapOf("host" to "http://localhost:9", "endpoint" to "/hook"))
        }

    "rejects missing, malformed and unknown credentials with a 401 error body" {
        registerOneTime().apply {
            status shouldBe HttpStatusCode.Unauthorized
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.MISSING_CREDENTIAL)
        }

        registerOneTime(authorization = "Bearer").apply {
            status shouldBe HttpStatusCode.Unauthorized
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.MISSING_CREDENTIAL)
        }

        registerOneTime(authorization = "Basic dXNlcjpwYXNz").apply {
            status shouldBe HttpStatusCode.Unauthorized
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.MISSING_CREDENTIAL)
        }

        registerOneTime(authorization = "Bearer dev-valoab-unknown").apply {
            status shouldBe HttpStatusCode.Unauthorized
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.INVALID_CREDENTIAL)
        }
    }

    "forbids the credential of a registered but inactive system with a 403 error body" {
        register(active = false)

        registerOneTime(authorization = "Bearer $token").apply {
            status shouldBe HttpStatusCode.Forbidden
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.INACTIVE_SYSTEM)
        }
    }

    "registers a task under the credential's system and rejects a conflicting caller claim" {
        register()

        val response = registerOneTime(authorization = "Bearer $token")
        response.status shouldBe HttpStatusCode.Created

        val id = (response.body<Map<String, Any>>()["id"] as Number).toInt()
        val task = getBean<HttpClient>().get("http://localhost:8080/tasks/$id").body<TaskResponse>()
        task.createdBy shouldBe systemName

        registerOneTime(authorization = "Bearer $token", caller = systemName).status shouldBe HttpStatusCode.Created

        registerOneTime(authorization = "Bearer $token", caller = "portfolio").apply {
            status shouldBe HttpStatusCode.Unauthorized
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.INVALID_CREDENTIAL)
        }
    }

    "honors rotation and deactivation by UPDATE without a restart" {
        register()

        registerOneTime(authorization = "Bearer $token").status shouldBe HttpStatusCode.Created

        setCredential(rotatedToken)
        registerOneTime(authorization = "Bearer $token").status shouldBe HttpStatusCode.Unauthorized
        registerOneTime(authorization = "Bearer $rotatedToken").status shouldBe HttpStatusCode.Created

        deactivate()
        registerOneTime(authorization = "Bearer $rotatedToken").apply {
            status shouldBe HttpStatusCode.Forbidden
            body<ErrorResponse>() shouldBe ErrorResponse(AuthorizationResult.INACTIVE_SYSTEM)
        }
    }
})
