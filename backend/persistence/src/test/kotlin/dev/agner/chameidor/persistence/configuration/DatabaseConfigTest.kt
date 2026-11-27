package dev.agner.chameidor.persistence.configuration

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DatabaseConfigTest : StringSpec({

    "resolves the configured port from the environment value" {
        resolveMysqlPort("8102") shouldBe 8102
    }

    "falls back to the canonical MySQL port when the environment value is absent" {
        resolveMysqlPort(null) shouldBe 8102
    }

    "falls back to the canonical MySQL port when the environment value is not a number" {
        resolveMysqlPort("not-a-port") shouldBe 8102
    }

    "builds the JDBC URL from the configured port instead of a hardcoded one" {
        mysqlJdbcUrl(host = "db", port = 8102) shouldBe "jdbc:mysql://db:8102/chameidor"
    }
})
