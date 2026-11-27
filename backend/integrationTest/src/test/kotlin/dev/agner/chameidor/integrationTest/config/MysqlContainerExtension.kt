package dev.agner.chameidor.integrationTest.config

import dev.agner.chameidor.persistence.configuration.DATABASE_NAME
import dev.agner.chameidor.persistence.migration.migrate
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.AfterProjectListener
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeProjectListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.testcontainers.containers.MySQLContainer
import java.net.ServerSocket

// One MySQL container and one app port per test JVM, so two concurrent suite runs never contend for
// a host port. The container is started before any spec and the coordinates it exposes are published
// as system properties — which outrank application.yaml — so DatabaseConfig, KtorConfig and
// AppHealthGateway follow the harness without a code change.
object MysqlContainerExtension : Extension, BeforeProjectListener, AfterProjectListener, AfterTestListener {

    override suspend fun beforeProject() {
        testMysql.start()
        publishCoordinates()
        // The container starts empty — db/migration/V*.sql is applied by Flyway, not by an init
        // script — so migrate it to head before any spec boots the Spring context.
        migrate(
            host = testMysql.host,
            port = testMysql.mappedPort,
            user = testMysql.username,
            password = testMysql.password,
        )
    }

    override suspend fun afterProject() {
        testMysql.stop()
    }

    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        val user = testMysql.username
        val database = testMysql.databaseName
        withContext(Dispatchers.IO) {
            testMysql.execInContainer(
                "bash",
                "-c",
                """
                    mysql -u$user -Nse 'show tables' $database | while read table; do
                       if [ "${'$'}table" != 'flyway_schema_history' ]; then
                           mysql -e "set FOREIGN_KEY_CHECKS=0; truncate table ${'$'}table" $database;
                       fi
                    done
                """.trimIndent(),
            ).exitCode shouldBe 0
        }
    }

    private fun publishCoordinates() {
        System.setProperty("mysql.host", testMysql.host)
        System.setProperty("mysql.port", testMysql.mappedPort.toString())
        System.setProperty("mysql.user", testMysql.username)
        System.setProperty("mysql.password", testMysql.password)
        System.setProperty("mysql.database", testMysql.databaseName)
        System.setProperty("ktor.port", freePort().toString())
    }
}

internal class MysqlTestContainer : MySQLContainer<MysqlTestContainer>("mysql:9.4.0") {
    init {
        withDatabaseName(DATABASE_NAME)
        withUsername("root")
        withPassword("")
        // testcontainers-java's bundled my.cnf still sets innodb_log_file_size, removed in MySQL
        // 8.0.30+ and rejected by mysql:9.4.0, so the container refuses to start without this
        // override. The file is empty on purpose: see src/test/resources/mysql-test-conf/my.cnf.
        withConfigurationOverride("mysql-test-conf")
    }
}

internal val testMysql = MysqlTestContainer()

internal val MysqlTestContainer.mappedPort: Int
    get() = getMappedPort(MySQLContainer.MYSQL_PORT)

// Probe-then-rebind has a millisecond-wide TOCTOU window; Testcontainers itself relies on
// OS-assigned ports the same way. The OS ephemeral range is disjoint from the dev block range.
private fun freePort() = ServerSocket(0).use { it.localPort }
