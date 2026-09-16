package dev.agner.chameidor.gateway.call

import dev.agner.chameidor.usecase.task.CallClient
import dev.agner.chameidor.usecase.task.EXTERNAL_SYSTEM_HEADER
import dev.agner.chameidor.usecase.task.TASK_ID_HEADER
import dev.agner.chameidor.usecase.task.Task
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CallGateway(
    private val client: HttpClient,
    @param:Value("\${chameidor.token}") private val token: String,
) : CallClient {

    override suspend fun makeCall(task: Task) =
        client
            .post("http://${task.host}${task.endpoint}") {
                timeout { requestTimeoutMillis = 600000 } // 10 minutes

                header(HttpHeaders.Authorization, "Bearer $token")
                header(EXTERNAL_SYSTEM_HEADER, task.createdBy)
                header(TASK_ID_HEADER, task.id.toString())

                task.data?.let {
                    contentType(ContentType.Application.Json)
                    setBody(it)
                }
            }
            .run {
                when {
                    status.isSuccess() ->
                        try {
                            body<Map<String, Any?>>()
                        } catch (e: Exception) {
                            null
                        }
                    else ->
                        throw IllegalStateException(
                            "Call to ${task.host}${task.endpoint} failed with status $status",
                        )
                }
            }
}
