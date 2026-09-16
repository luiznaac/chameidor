package dev.agner.chameidor.integrationTest.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import dev.agner.chameidor.usecase.configuration.JsonMapper

object HttpMockService {

    private val server = WireMockServer(3000)
    private val mapper = JsonMapper.jsonAdapter()

    init {
        server.start()
    }

    class ResponseConfiguration {
        lateinit var method: RequestMethod
        lateinit var endpoint: String
        var queryParams: Map<String, String> = emptyMap()
        var headers: Map<String, String> = emptyMap()
        var httpStatus: Int = 200
        var payload: Any = emptyMap<String, String>()
    }

    class ResponseScope {
        val responses = mutableSetOf<ResponseConfiguration>()

        suspend fun response(configure: suspend ResponseConfiguration.() -> Unit) {
            responses += ResponseConfiguration().apply { configure() }
        }
    }

    suspend fun configureResponses(scope: suspend ResponseScope.() -> Unit) {
        ResponseScope()
            .apply { scope() }
            .responses
            .forEach { configuration ->
                server.stubFor(
                    WireMock.request(configuration.method.value(), urlPathMatching(configuration.endpoint))
                        .apply {
                            configuration.queryParams.forEach { (key, value) -> withQueryParam(key, equalTo(value)) }
                            configuration.headers.forEach { (key, value) -> withHeader(key, equalTo(value)) }
                        }
                        .willReturn(
                            WireMock.aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withStatus(configuration.httpStatus)
                                .withBody(mapper.writeValueAsBytes(configuration.payload)),
                        ),
                )
            }
    }

    fun verify(method: RequestMethod, endpoint: String, headers: Map<String, String>) {
        val pattern = RequestPatternBuilder.newRequestPattern(method, urlPathMatching(endpoint))
            .apply { headers.forEach { (key, value) -> withHeader(key, equalTo(value)) } }

        server.verify(pattern)
    }

    fun clearMocks() {
        server.resetAll()
    }
}
