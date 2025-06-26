package data

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

object Users : IntIdTable("users") {
    val telegramId = long("telegram_id").uniqueIndex()
}

object Exercises : IntIdTable("exercises") {
    val user = reference("user_id", Users)
    val name = varchar("name", 100)
}

object Workouts : IntIdTable("workouts") {
    val user = reference("user_id", Users)
    val date = datetime("date")
}

object Sets : IntIdTable("sets") {
    val workout = reference("workout_id", Workouts)
    val exercise = reference("exercise_id", Exercises)
    val reps = integer("reps")
    val weight = integer("weight").nullable()
    val setIndex = integer("set_index")
} 