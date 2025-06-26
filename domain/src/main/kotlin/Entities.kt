package domain

import java.time.LocalDateTime

data class User(val id: Int, val telegramId: Long)
data class Exercise(val id: Int, val userId: Int, val name: String)
data class Workout(val id: Int, val userId: Int, val date: LocalDateTime)
data class WorkoutSet(val id: Int, val workoutId: Int, val exerciseId: Int, val reps: Int, val weight: Int?, val setIndex: Int)
data class WorkoutWithSets(val workout: Workout, val sets: List<WorkoutSet>)

data class SwimmingWorkout(
    val id: Int,
    val userId: Int,
    val distance: Int,
    val totalTime: Int,
    val paddlesDistance: Int?,
    val best50mTime: Int?,
    val date: LocalDateTime
)

data class PullUpWorkout(
    val id: Int,
    val userId: Int,
    val totalPullUps: Int,
    val maxPullUpsInSet: Int,
    val date: LocalDateTime
) 