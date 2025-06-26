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
    fun init(databaseUrl: String, user: String, password: String) {
        Database.connect(
            url = databaseUrl,
            driver = "org.postgresql.Driver",
            user = user,
            password = password
        )
        transaction {
            SchemaUtils.create(Users, Exercises, Workouts, Sets)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
} 