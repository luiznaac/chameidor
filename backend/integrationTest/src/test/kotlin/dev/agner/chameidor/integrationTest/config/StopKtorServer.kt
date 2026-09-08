package dev.agner.chameidor.integrationTest.config

import dev.agner.chameidor.httpapi.configuration.KtorConfig
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestExecutionListener

class StopKtorServer : TestExecutionListener {
    override fun afterTestMethod(testContext: TestContext) {
        testContext.applicationContext
            .autowireCapableBeanFactory
            .getBean(KtorConfig::class.java).stop()
    }
}
