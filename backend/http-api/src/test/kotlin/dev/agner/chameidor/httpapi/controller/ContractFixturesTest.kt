package dev.agner.chameidor.httpapi.controller

import dev.agner.chameidor.usecase.configuration.JsonMapper
import dev.agner.chameidor.usecase.task.Task
import dev.agner.chameidor.usecase.task.TaskCreation.OneTimeTaskCreation
import dev.agner.chameidor.usecase.task.TaskCreation.PeriodicTaskCreation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.springframework.scheduling.support.CronExpression
import java.io.File

// Golden request/response fixtures published in contracts/ for other apps to pin. A DTO change
// breaks these tests — update the DTO and the fixture together (see contracts/README.md).
class ContractFixturesTest : StringSpec({

    val mapper = JsonMapper.jsonAdapter()

    "the published one-time request fixture deserialises to the endpoint's request type" {
        val request = mapper.readValue(
            contractsDir().resolve("tasks-one-time-request.json"),
            OneTimeTaskCreation::class.java,
        )

        request.host shouldBe "portfolio:8080"
        request.endpoint shouldBe "/consolidations/BOND/1"
        request.data shouldBe null
    }

    "the published periodic request fixture deserialises with its 5-field cron" {
        val request = mapper.readValue(
            contractsDir().resolve("tasks-periodic-request.json"),
            PeriodicTaskCreation::class.java,
        )

        request.host shouldBe "portfolio:8080"
        request.endpoint shouldBe "/consolidations/BOND/1"
        request.cron.toString() shouldBe "0 */5 * * * *"
    }

    "the endpoint serialises a one-time task to exactly the published response fixture" {
        val task = Task.OneTimeTask(
            id = 1,
            host = "portfolio:8080",
            endpoint = "/consolidations/BOND/1",
            data = null,
            createdBy = "valoab",
        )

        mapper.readTree(mapper.writeValueAsBytes(TaskRegistrationResponse.from(task))) shouldBe
            mapper.readTree(contractsDir().resolve("tasks-one-time-response.json"))
    }

    "the endpoint serialises a periodic task to exactly the published response fixture" {
        val task = Task.PeriodicTask(
            id = 1,
            host = "portfolio:8080",
            endpoint = "/consolidations/BOND/1",
            data = null,
            createdBy = "valoab",
            cron = CronExpression.parse("0 */5 * * * *"),
        )

        mapper.readTree(mapper.writeValueAsBytes(TaskRegistrationResponse.from(task))) shouldBe
            mapper.readTree(contractsDir().resolve("tasks-periodic-response.json"))
    }
})

private fun contractsDir(): File =
    File(
        System.getProperty("contractsDir")
            ?: error("contractsDir system property is not set; see backend/build.gradle.kts"),
    )
