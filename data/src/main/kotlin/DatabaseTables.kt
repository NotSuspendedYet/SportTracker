package data

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

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

object SwimmingWorkouts : IntIdTable("swimming_workouts") {
    val user = reference("user_id", Users)
    val distance = integer("distance")
    val totalTime = integer("total_time")
    val paddlesDistance = integer("paddles_distance").nullable()
    val best50mTime = integer("best_50m_time").nullable()
    val date = datetime("date")
}

object PullUpWorkouts : IntIdTable("pullup_workouts") {
    val user = reference("user_id", Users)
    val totalPullUps = integer("total_pullups")
    val maxPullUpsInSet = integer("max_pullups_in_set")
    val date = datetime("date")
}

object AbsWorkouts : IntIdTable("abs_workouts") {
    val user = reference("user_id", Users)
    val date = datetime("date")
} 