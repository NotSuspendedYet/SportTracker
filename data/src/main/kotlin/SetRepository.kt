package data

import domain.Set

interface SetRepository {
    suspend fun addSet(workoutId: Int, exerciseId: Int, reps: Int, weight: Int?, setIndex: Int): Set
}

class SetRepositoryImpl : SetRepository {
    override suspend fun addSet(workoutId: Int, exerciseId: Int, reps: Int, weight: Int?, setIndex: Int): Set = DatabaseFactory.dbQuery {
        val id = Sets.insertAndGetId {
            it[Sets.workout] = workoutId
            it[Sets.exercise] = exerciseId
            it[Sets.reps] = reps
            it[Sets.weight] = weight
            it[Sets.setIndex] = setIndex
        }.value
        Set(id, workoutId, exerciseId, reps, weight, setIndex)
    }
} 