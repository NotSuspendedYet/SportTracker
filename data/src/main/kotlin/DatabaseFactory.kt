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

object DatabaseFactory {
    fun init(databaseUrl: String) {
        // Render URL is postgresql://, JDBC needs jdbc:postgresql://
        val jdbcUrl = "jdbc:" + databaseUrl
        Database.connect(url = jdbcUrl, driver = "org.postgresql.Driver")
        
        transaction {
            SchemaUtils.create(Users, Exercises, Workouts, Sets)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
} 