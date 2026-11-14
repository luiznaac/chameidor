package dev.agner.chameidor.httpapi.configuration

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond

/**
 * The boundary that owns an unexpected failure's response. `RequestTelemetry` already logged
 * the single ERROR record, so this handler consumes the failure and answers 500 instead of
 * letting Ktor's default uncaught-exception logger emit a second record.
 */
fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}
