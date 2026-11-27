package dev.agner.chameidor.persistence.configuration

import org.jetbrains.exposed.v1.jdbc.Database
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

const val DATABASE_NAME = "chameidor"

// Local-dev block role `mysql`. The port the standalone tooling (Flyway's `main`, the integration
// harness) resolves when `MYSQL_PORT` is unset or not a number.
const val DEFAULT_MYSQL_PORT = 8102

// Shared by the Exposed connection below and by the Flyway migrator (persistence/migration/), which
// runs in its own process before the app boots and so cannot read the Spring context. One
// definition, so the two can't drift apart.
fun mysqlJdbcUrl(host: String, port: Int) = "jdbc:mysql://$host:$port/$DATABASE_NAME"

internal fun resolveMysqlPort(raw: String?) = raw?.toIntOrNull() ?: DEFAULT_MYSQL_PORT

@Configuration
class DatabaseConfig {

    @Bean
    fun databaseConnection(
        @Value("\${mysql.host}") host: String,
        @Value("\${mysql.port}") port: Int,
        @Value("\${mysql.user}") user: String,
        @Value("\${mysql.password}") password: String,
    ) = Database.connect(
        url = mysqlJdbcUrl(host = host, port = port),
        driver = "com.mysql.cj.jdbc.Driver",
        user = user,
        password = password,
    )
}
