package dev.agner.chameidor.persistence.migration

import dev.agner.chameidor.persistence.configuration.mysqlJdbcUrl
import dev.agner.chameidor.persistence.task.TaskExecutionTable
import dev.agner.chameidor.persistence.task.TaskHistoryTable
import dev.agner.chameidor.persistence.task.TaskTable
import org.jetbrains.exposed.v1.core.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

const val MIGRATIONS_DIRECTORY = "src/main/resources/db/migration"

// Every table this service maps, in dependency order. Single source for the two things that need
// the whole set: MigrationSchemaTest (which diffs it against a migrated database) and this file's
// own `generateMigrationScript` entry point. A new *Table object must be added here.
val allTables: Array<Table> = arrayOf(TaskTable, TaskHistoryTable, TaskExecutionTable)

// Authoring half of the migration workflow (backend/AGENTS.md). Diffs `allTables` against a
// local database already migrated to head and writes the SQL that closes the gap into
// db/migration/. Exposed only *generates* — Flyway applies. Always read the output before
// committing it: the diff is mechanical and won't, for instance, know that a rename is a rename
// rather than a drop plus an add.
@OptIn(ExperimentalDatabaseMigrationApi::class)
fun main() {
    val name = System.getProperty("migration.name").orEmpty()
    require(name.isNotBlank()) { "pass the migration name: ./gradlew generateMigrationScript -Pname=V2__add_x" }

    val mysql = MysqlEnv.fromEnvironment()
    Database.connect(
        url = mysqlJdbcUrl(mysql.host),
        driver = "com.mysql.cj.jdbc.Driver",
        user = mysql.user,
        password = mysql.password,
    )

    val script = transaction {
        MigrationUtils.generateMigrationScript(
            *allTables,
            scriptDirectory = MIGRATIONS_DIRECTORY,
            scriptName = name,
        )
    }
    println("wrote ${script.absolutePath}")
}
