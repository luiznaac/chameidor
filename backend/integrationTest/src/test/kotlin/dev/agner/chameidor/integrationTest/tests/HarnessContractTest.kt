package dev.agner.chameidor.integrationTest.tests

import dev.agner.chameidor.integrationTest.config.HttpMockService
import dev.agner.chameidor.integrationTest.config.IntegrationTest
import dev.agner.chameidor.integrationTest.config.getRequest
import dev.agner.chameidor.integrationTest.config.mappedPort
import dev.agner.chameidor.integrationTest.config.testMysql
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

// Pins the harness contract: the suite takes its ports from the OS and its database coordinates
// from its own container, never the canonical dev block. A regression here only shows up as a
// collision once two runs overlap, which is exactly when it is hardest to diagnose.
@IntegrationTest
class HarnessContractTest : StringSpec({

    "the app and its self-health follow the probed app port" {
        val checks = getRequest<List<Map<String, Any?>>> { path = "/health" }

        checks.map { it["service_name"] } shouldContainExactlyInAnyOrder
            listOf("mysql-connection", "http-client", "task-job")
        for (check in checks) {
            check["is_healthy"] shouldBe true
        }
    }

    "the harness probes a free app port instead of binding the dev default" {
        System.getProperty("ktor.port") shouldNotBe null
        System.getProperty("ktor.port") shouldNotBe "8100"
    }

    "the harness publishes the container's database coordinates" {
        System.getProperty("mysql.host") shouldBe testMysql.host
        System.getProperty("mysql.port") shouldBe testMysql.mappedPort.toString()
        System.getProperty("mysql.user") shouldBe testMysql.username
        System.getProperty("mysql.password") shouldBe testMysql.password
        System.getProperty("mysql.database") shouldBe "chameidor"
    }

    "WireMock binds an OS-assigned port and exposes it" {
        HttpMockService.baseUrl shouldNotBe "http://localhost:3000"
        HttpMockService.baseUrl shouldMatch "http://localhost:\\d+".toRegex()
    }
})
