package domain

/**
 * Парсит строку вида "12x3@50, 15x2" в список Triple<reps, count, weight?>
 * Например: "12x3@50" -> Triple(12, 3, 50)
 *           "15x2"    -> Triple(15, 2, null)
 */
object WorkoutParser {
    private val regex = Regex("(\\d+)x(\\d+)(?:@([\\d.]+))?")

    fun parseSets(input: String): List<Triple<Int, Int, Int?>> {
        return input.split(",").mapNotNull { part ->
            val match = regex.find(part.trim()) ?: return@mapNotNull null
            val (reps, count, weight) = match.destructured
            Triple(reps.toInt(), count.toInt(), weight.takeIf { it.isNotEmpty() }?.toIntOrNull())
        }
    }
} 