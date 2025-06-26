package bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId.Companion.fromId
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.entities.keyboard.ReplyKeyboardMarkup
import com.github.kotlintelegrambot.logging.LogLevel
import data.DatabaseFactory
import data.ExerciseRepositoryImpl
import data.SetRepositoryImpl
import data.UserRepositoryImpl
import data.WorkoutRepositoryImpl
import domain.WorkoutParser
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

private val mainMenu = ReplyKeyboardMarkup(
    keyboard = listOf(
        listOf(
            KeyboardButton(text = "➕ Добавить упражнение"),
            KeyboardButton(text = "🏋️ Записать тренировку")
        ),
        listOf(
            KeyboardButton(text = "📊 Отчёт")
        )
    ),
    resizeKeyboard = true
)

fun main() {
    val botToken = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN not set")
    val dbUrl = System.getenv("DATABASE_URL") ?: error("DATABASE_URL not set")

    DatabaseFactory.init(dbUrl)
    val userRepo = UserRepositoryImpl()
    val exerciseRepo = ExerciseRepositoryImpl()
    val workoutRepo = WorkoutRepositoryImpl()
    val setRepo = SetRepositoryImpl()

    val dialogState = ConcurrentHashMap<Long, DialogState>()

    val bot = Bot.Builder()
        .apply {
            token = botToken
            logLevel = LogLevel.Network.Body
            dispatch {
                command("start") {
                    bot.sendMessage(
                        chatId = fromId(message.chat.id),
                        text = "Привет! Я помогу отслеживать твои тренировки. Выбери действие из меню.",
                        replyMarkup = mainMenu
                    )
                }
                text {
                    val userId = message.from?.id ?: return@text
                    val state = dialogState[userId]
                    when {
                        text == "➕ Добавить упражнение" -> {
                            dialogState[userId] = DialogState.AddExercise
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Введи название упражнения:",
                                replyMarkup = mainMenu
                            )
                        }
                        text == "🏋️ Записать тренировку" -> {
                            val user = userRepo.getOrCreateByTelegramId(userId)
                            val exercises = exerciseRepo.getExercisesByUser(user.id)
                            if (exercises.isEmpty()) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Сначала добавь упражнение через меню",
                                    replyMarkup = mainMenu
                                )
                            } else {
                                val list = exercises.mapIndexed { i, ex -> "${i + 1}. ${ex.name}" }.joinToString("\n")
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Выбери упражнение:\n$list",
                                    replyMarkup = mainMenu
                                )
                                dialogState[userId] = DialogState.RecordWorkout_SelectExercise(exercises.first().id) // TODO: выбор по номеру
                            }
                        }
                        text == "📊 Отчёт" -> {
                            val user = userRepo.getOrCreateByTelegramId(userId)
                            val workouts = workoutRepo.getWorkoutsByUser(user.id, null, null)
                            if (workouts.isEmpty()) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Нет записей о тренировках.",
                                    replyMarkup = mainMenu
                                )
                            } else {
                                val report = workouts.joinToString("\n\n") { w ->
                                    val date = w.workout.date.truncatedTo(ChronoUnit.MINUTES)
                                    val sets = w.sets.joinToString("; ") { s -> "${s.reps}x${s.setIndex}${s.weight?.let { "@${it}" } ?: ""}" }
                                    "$date: $sets"
                                }
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = report,
                                    replyMarkup = mainMenu
                                )
                            }
                        }
                        state is DialogState.AddExercise -> {
                            val user = userRepo.getOrCreateByTelegramId(userId)
                            exerciseRepo.addExercise(user.id, text)
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Упражнение '$text' добавлено!",
                                replyMarkup = mainMenu
                            )
                            dialogState.remove(userId)
                        }
                        state is DialogState.RecordWorkout_SelectExercise -> {
                            val exerciseId = state.exerciseId
                            dialogState[userId] = DialogState.RecordWorkout_EnterSets(exerciseId)
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Введи подходы (например: 12x3@50, 15x2)",
                                replyMarkup = mainMenu
                            )
                        }
                        state is DialogState.RecordWorkout_EnterSets -> {
                            val user = userRepo.getOrCreateByTelegramId(userId)
                            val exerciseId = state.exerciseId
                            val setsParsed = WorkoutParser.parseSets(text)
                            val workout = workoutRepo.addWorkout(user.id, LocalDateTime.now())
                            var setIndex = 1
                            setsParsed.forEach { (reps, count, weight) ->
                                repeat(count) {
                                    setRepo.addSet(workout.id, exerciseId, reps, weight, setIndex)
                                    setIndex++
                                }
                            }
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Тренировка записана!",
                                replyMarkup = mainMenu
                            )
                            dialogState.remove(userId)
                        }
                        else -> {
                            // Не отвечаем на произвольный текст, если не в диалоге и не кнопка
                        }
                    }
                }
                callbackQuery {
                    // Для inline-кнопок, если потребуется
                }
                // Оставляем команды для совместимости с ручным вводом
                command("add_exercise") {
                    dialogState[message.from!!.id] = DialogState.AddExercise
                    bot.sendMessage(
                        chatId = fromId(message.chat.id),
                        text = "Введи название упражнения:",
                        replyMarkup = mainMenu
                    )
                }
                command("record_workout") {
                    val user = userRepo.getOrCreateByTelegramId(message.from!!.id)
                    val exercises = exerciseRepo.getExercisesByUser(user.id)
                    if (exercises.isEmpty()) {
                        bot.sendMessage(
                            chatId = fromId(message.chat.id),
                            text = "Сначала добавь упражнение через меню",
                            replyMarkup = mainMenu
                        )
                    } else {
                        val list = exercises.mapIndexed { i, ex -> "${i + 1}. ${ex.name}" }.joinToString("\n")
                        bot.sendMessage(
                            chatId = fromId(message.chat.id),
                            text = "Выбери упражнение:\n$list",
                            replyMarkup = mainMenu
                        )
                        dialogState[message.from!!.id] = DialogState.RecordWorkout_SelectExercise(exercises.first().id) // TODO: выбор по номеру
                    }
                }
                command("report") {
                    val user = userRepo.getOrCreateByTelegramId(message.from!!.id)
                    val workouts = workoutRepo.getWorkoutsByUser(user.id, null, null)
                    if (workouts.isEmpty()) {
                        bot.sendMessage(
                            chatId = fromId(message.chat.id),
                            text = "Нет записей о тренировках.",
                            replyMarkup = mainMenu
                        )
                    } else {
                        val report = workouts.joinToString("\n\n") { w ->
                            val date = w.workout.date.truncatedTo(ChronoUnit.MINUTES)
                            val sets = w.sets.joinToString("; ") { s -> "${s.reps}x${s.setIndex}${s.weight?.let { "@${it}" } ?: ""}" }
                            "$date: $sets"
                        }
                        bot.sendMessage(
                            chatId = fromId(message.chat.id),
                            text = report,
                            replyMarkup = mainMenu
                        )
                    }
                }
            }
        }
        .build()

    embeddedServer(Netty, System.getenv("PORT")?.toInt() ?: 8080) {
        install(ContentNegotiation) { json() }
        routing {
            get("/") { call.respondText("SportTracker Bot is running!") }
        }
        launch {
            bot.startPolling()
        }
        // TODO: добавить периодические задачи для уведомлений
    }.start(wait = true)
}

sealed class DialogState {
    data object AddExercise : DialogState()
    data class RecordWorkout_SelectExercise(val exerciseId: Int) : DialogState()
    data class RecordWorkout_EnterSets(val exerciseId: Int) : DialogState()
} 