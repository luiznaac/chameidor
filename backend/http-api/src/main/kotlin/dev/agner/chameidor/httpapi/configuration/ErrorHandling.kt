package dev.agner.chameidor.httpapi.configuration

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.defaultExceptionStatusCode
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.response.respond

/**
 * The boundary that owns an unexpected failure's response. `RequestTelemetry` already logged
 * the single boundary record, so this handler consumes the failure instead of letting Ktor's
 * default uncaught-exception logger emit a second record. Ktor's own status mapping keeps a
 * framework-rejected request a 4xx/504; an unexpected failure stays a bare 500.
 */
fun Application.installErrorHandling() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(defaultExceptionStatusCode(cause) ?: HttpStatusCode.InternalServerError)
        }
    }
}
