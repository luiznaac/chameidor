package dev.agner.chameidor.gateway.call

import dev.agner.chameidor.usecase.configuration.JsonMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import java.io.File

// callback-request.json is the body of the POST chameidor makes to consumers when a task runs.
// This asserts the real CallGateway payload against the published fixture (contracts/README.md).
class CallGatewayContractTest : StringSpec({

    "posts the registered data as exactly the published callback fixture" {
        val fixture = contractsDir().resolve("callback-request.json")
        val data = JsonMapper.jsonAdapter().readValue(fixture, Map::class.java)

        val request = captureRequest { it.makeCall(HOST, ENDPOINT, data) }

        request.method shouldBe HttpMethod.Post
        request.url.toString() shouldBe "http://$HOST$ENDPOINT"
        request.body.contentType?.match(ContentType.Application.Json) shouldBe true
        JsonMapper.jsonAdapter().readTree(request.bodyText()) shouldBe
            JsonMapper.jsonAdapter().readTree(fixture)
    }

    "sends no body and no content type when the task carries no data" {
        val request = captureRequest { it.makeCall(HOST, ENDPOINT, null) }

        request.headers[HttpHeaders.ContentType] shouldBe null
        request.bodyText() shouldBe ""
    }
})

private const val HOST = "portfolio:8080"
private const val ENDPOINT = "/consolidations/BOND/1"

private suspend fun captureRequest(block: suspend (CallGateway) -> Unit): HttpRequestData {
    lateinit var captured: HttpRequestData
    val engine = MockEngine { request ->
        captured = request
        respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val client = HttpClient(engine) {
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(JsonMapper.jsonAdapter()))
        }
    }

    block(CallGateway(client))
    return captured
}

private suspend fun HttpRequestData.bodyText(): String = body.toByteArray().decodeToString()

private fun contractsDir(): File =
    File(
        System.getProperty("contractsDir")
            ?: error("contractsDir system property is not set; see backend/build.gradle.kts"),
    )
