package data

import domain.User
import domain.Exercise
import domain.Workout
import domain.WorkoutSet
import domain.WorkoutWithSets
import domain.SwimmingWorkout
import domain.PullUpWorkout
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

interface UserRepository {
    suspend fun getOrCreateByTelegramId(telegramId: Long): User
    suspend fun getById(id: Int): User?
}

interface ExerciseRepository {
    suspend fun addExercise(userId: Int, name: String): Exercise
    suspend fun getExercisesByUser(userId: Int): List<Exercise>
    suspend fun getById(id: Int): Exercise?
}

interface WorkoutRepository {
    suspend fun addWorkout(userId: Int, date: LocalDateTime): Workout
    suspend fun getWorkoutsByUser(userId: Int, from: LocalDateTime?, to: LocalDateTime?): List<WorkoutWithSets>
}

interface SwimmingWorkoutRepository {
    suspend fun addSwimmingWorkout(userId: Int, distance: Int, totalTime: Int, paddlesDistance: Int?, best50mTime: Int?, date: LocalDateTime): SwimmingWorkout
    suspend fun getAllByUser(userId: Int): List<SwimmingWorkout>
}

interface PullUpWorkoutRepository {
    suspend fun addPullUpWorkout(userId: Int, totalPullUps: Int, maxPullUpsInSet: Int, date: LocalDateTime): PullUpWorkout
    suspend fun getAllByUser(userId: Int): List<PullUpWorkout>
}

class UserRepositoryImpl : UserRepository {
    override suspend fun getOrCreateByTelegramId(telegramId: Long): User = DatabaseFactory.dbQuery {
        val userRow = Users.select { Users.telegramId eq telegramId }.singleOrNull()
        if (userRow != null) {
            User(userRow[Users.id].value, userRow[Users.telegramId])
        } else {
            val id = Users.insertAndGetId {
                it[Users.telegramId] = telegramId
            }.value
            User(id, telegramId)
        }
    }

    override suspend fun getById(id: Int): User? = DatabaseFactory.dbQuery {
        Users.select { Users.id eq id }.singleOrNull()?.let {
            User(it[Users.id].value, it[Users.telegramId])
        }
    }
}

class ExerciseRepositoryImpl : ExerciseRepository {
    override suspend fun addExercise(userId: Int, name: String): Exercise = DatabaseFactory.dbQuery {
        val id = Exercises.insertAndGetId {
            it[Exercises.user] = userId
            it[Exercises.name] = name
        }.value
        Exercise(id, userId, name)
    }

    override suspend fun getExercisesByUser(userId: Int): List<Exercise> = DatabaseFactory.dbQuery {
        Exercises.select { Exercises.user eq userId }.map {
            Exercise(it[Exercises.id].value, it[Exercises.user].value, it[Exercises.name])
        }
    }

    override suspend fun getById(id: Int): Exercise? = DatabaseFactory.dbQuery {
        Exercises.select { Exercises.id eq id }.singleOrNull()?.let {
            Exercise(it[Exercises.id].value, it[Exercises.user].value, it[Exercises.name])
        }
    }
}

class WorkoutRepositoryImpl : WorkoutRepository {
    override suspend fun addWorkout(userId: Int, date: LocalDateTime): Workout = DatabaseFactory.dbQuery {
        val id = Workouts.insertAndGetId {
            it[Workouts.user] = userId
            it[Workouts.date] = date
        }.value
        Workout(id, userId, date)
    }

    override suspend fun getWorkoutsByUser(userId: Int, from: LocalDateTime?, to: LocalDateTime?): List<WorkoutWithSets> = DatabaseFactory.dbQuery {
        Workouts.select { Workouts.user eq userId }
            .filter { workoutRow ->
                (from == null || workoutRow[Workouts.date] >= from) && 
                (to == null || workoutRow[Workouts.date] <= to)
            }
            .map { workoutRow ->
                val workout = Workout(workoutRow[Workouts.id].value, workoutRow[Workouts.user].value, workoutRow[Workouts.date])
                val sets = Sets.select { Sets.workout eq workout.id }.map { setRow ->
                    WorkoutSet(
                        setRow[Sets.id].value,
                        setRow[Sets.workout].value,
                        setRow[Sets.exercise].value,
                        setRow[Sets.reps],
                        setRow[Sets.weight],
                        setRow[Sets.setIndex]
                    )
                }
                WorkoutWithSets(workout, sets)
            }
    }
}

class SwimmingWorkoutRepositoryImpl : SwimmingWorkoutRepository {
    override suspend fun addSwimmingWorkout(userId: Int, distance: Int, totalTime: Int, paddlesDistance: Int?, best50mTime: Int?, date: LocalDateTime): SwimmingWorkout = DatabaseFactory.dbQuery {
        val id = SwimmingWorkouts.insertAndGetId {
            it[SwimmingWorkouts.user] = userId
            it[SwimmingWorkouts.distance] = distance
            it[SwimmingWorkouts.totalTime] = totalTime
            it[SwimmingWorkouts.paddlesDistance] = paddlesDistance
            it[SwimmingWorkouts.best50mTime] = best50mTime
            it[SwimmingWorkouts.date] = date
        }.value
        SwimmingWorkout(id, userId, distance, totalTime, paddlesDistance, best50mTime, date)
    }

    override suspend fun getAllByUser(userId: Int): List<SwimmingWorkout> = DatabaseFactory.dbQuery {
        SwimmingWorkouts.select { SwimmingWorkouts.user eq userId }.map {
            SwimmingWorkout(
                it[SwimmingWorkouts.id].value,
                it[SwimmingWorkouts.user].value,
                it[SwimmingWorkouts.distance],
                it[SwimmingWorkouts.totalTime],
                it[SwimmingWorkouts.paddlesDistance],
                it[SwimmingWorkouts.best50mTime],
                it[SwimmingWorkouts.date]
            )
        }
    }
}

class PullUpWorkoutRepositoryImpl : PullUpWorkoutRepository {
    override suspend fun addPullUpWorkout(userId: Int, totalPullUps: Int, maxPullUpsInSet: Int, date: LocalDateTime): PullUpWorkout = DatabaseFactory.dbQuery {
        val id = PullUpWorkouts.insertAndGetId {
            it[PullUpWorkouts.user] = userId
            it[PullUpWorkouts.totalPullUps] = totalPullUps
            it[PullUpWorkouts.maxPullUpsInSet] = maxPullUpsInSet
            it[PullUpWorkouts.date] = date
        }.value
        PullUpWorkout(id, userId, totalPullUps, maxPullUpsInSet, date)
    }

    override suspend fun getAllByUser(userId: Int): List<PullUpWorkout> = DatabaseFactory.dbQuery {
        PullUpWorkouts.select { PullUpWorkouts.user eq userId }.map {
            PullUpWorkout(
                it[PullUpWorkouts.id].value,
                it[PullUpWorkouts.user].value,
                it[PullUpWorkouts.totalPullUps],
                it[PullUpWorkouts.maxPullUpsInSet],
                it[PullUpWorkouts.date]
            )
        }
    }
} 