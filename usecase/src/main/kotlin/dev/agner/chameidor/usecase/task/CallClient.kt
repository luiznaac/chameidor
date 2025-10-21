package dev.agner.chameidor.usecase.task

interface CallClient {

    suspend fun makeCall(host: String, endpoint: String, data: Any?): Any?
}
