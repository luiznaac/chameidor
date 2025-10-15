package dev.agner.chameidor.gateway.call

import dev.agner.chameidor.usecase.task.CallClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.stereotype.Component

@Component
class CallGateway(
    private val client: HttpClient,
) : CallClient {

    override suspend fun makeCall(host: String, endpoint: String, data: Any?) =
        client
            .post("http://$host$endpoint") {
                timeout { requestTimeoutMillis = 600000 } // 10 minutes

                data?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }
            .body<Any?>()
}
