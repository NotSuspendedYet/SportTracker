package data

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlinx.coroutines.Dispatchers
import data.Users
import data.Exercises
import data.Workouts
import data.Sets
import data.SwimmingWorkouts
import data.PullUpWorkouts

object DatabaseFactory {
    fun init(databaseUrl: String) {
        // Преобразуем Render-URL в JDBC-URL
        // postgresql://user:password@host:port/db
        val regex = Regex("""postgres(?:ql)?://([^:]+):([^@]+)@([^:/]+)(?::(\d+))?/([^?]+)""")
        val match = regex.matchEntire(databaseUrl)
            ?: error("Invalid DATABASE_URL format: $databaseUrl")
        val (user, password, host, port, db) = match.destructured
        val portPart = if (port.isNotEmpty()) ":$port" else ""
        val jdbcUrl = "jdbc:postgresql://$host$portPart/$db?user=$user&password=$password&sslmode=require"

        Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver")
        transaction {
            SchemaUtils.create(Users, Exercises, Workouts, Sets, SwimmingWorkouts, PullUpWorkouts)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
} 