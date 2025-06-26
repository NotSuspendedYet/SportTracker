package data

import domain.WorkoutSet

interface SetRepository {
    suspend fun addSet(workoutId: Int, exerciseId: Int, reps: Int, weight: Int?, setIndex: Int): WorkoutSet
}

class SetRepositoryImpl : SetRepository {
    override suspend fun addSet(workoutId: Int, exerciseId: Int, reps: Int, weight: Int?, setIndex: Int): WorkoutSet = DatabaseFactory.dbQuery {
        val id = Sets.insertAndGetId {
            it[Sets.workout] = workoutId
            it[Sets.exercise] = exerciseId
            it[Sets.reps] = reps
            it[Sets.weight] = weight
            it[Sets.setIndex] = setIndex
        }.value
        WorkoutSet(id, workoutId, exerciseId, reps, weight, setIndex)
    }
} 