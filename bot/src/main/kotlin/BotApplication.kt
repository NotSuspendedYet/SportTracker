package bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId.Companion.fromId
import com.github.kotlintelegrambot.entities.KeyboardReplyMarkup
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import data.AbsWorkoutRepositoryImpl
import data.DatabaseFactory
import data.PullUpWorkoutRepositoryImpl
import data.SwimmingWorkoutRepositoryImpl
import data.UserRepositoryImpl
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
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

private val mainMenu = KeyboardReplyMarkup(
    keyboard = listOf(
        listOf(
            KeyboardButton(text = "🏊 Бассейн"),
            KeyboardButton(text = "🏋️ Турник"),
            KeyboardButton(text = "🦾 Пресс")
        ),
        listOf(
            KeyboardButton(text = "📊 Отчёт")
        )
    ),
    resizeKeyboard = true
)

private val cancelMenu = KeyboardReplyMarkup(
    keyboard = listOf(
        listOf(KeyboardButton(text = "❌ Отмена"))
    ),
    resizeKeyboard = true
)

fun main() {
    val botToken = System.getenv("BOT_TOKEN") ?: error("BOT_TOKEN not set")
    val dbUrl = System.getenv("DATABASE_URL") ?: error("DATABASE_URL not set")

    DatabaseFactory.init(dbUrl)
    val userRepo = UserRepositoryImpl()
    val swimmingRepo = SwimmingWorkoutRepositoryImpl()
    val pullupRepo = PullUpWorkoutRepositoryImpl()
    val absRepo = AbsWorkoutRepositoryImpl()

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
                        text == "🏊 Бассейн" -> {
                            dialogState[userId] = DialogState.Swim_Distance
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Введи дистанцию (в метрах):",
                                replyMarkup = cancelMenu
                            )
                        }
                        text == "🏋️ Турник" -> {
                            dialogState[userId] = DialogState.PullUp_Total
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Введи общее число подтягиваний:",
                                replyMarkup = cancelMenu
                            )
                        }
                        text == "🦾 Пресс" -> {
                            val user = userRepo.getOrCreateByTelegramId(userId)
                            absRepo.addAbsWorkout(user.id, LocalDateTime.now())
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Тренировка на пресс сохранена!",
                                replyMarkup = mainMenu
                            )
                        }
                        text == "📊 Отчёт" -> {
                            val user = userRepo.getOrCreateByTelegramId(userId)
                            val swimList = swimmingRepo.getAllByUser(user.id)
                            val pullupList = pullupRepo.getAllByUser(user.id)
                            val absList = absRepo.getAllByUser(user.id)
                            val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

                            val swimReport = if (swimList.isEmpty()) "Нет записей по бассейну." else swimList.joinToString("\n\n") {
                                "${it.date.format(formatter)}\nДистанция: ${it.distance} м\nВремя: ${it.formattedTotalTime}" +
                                (it.paddlesDistance?.let { pd -> "\nС лопатками: $pd м" } ?: "") +
                                (it.best50mTime?.let { b50 -> "\nЛучшие 50м: $b50 сек" } ?: "")
                            }
                            val pullupReport = if (pullupList.isEmpty()) "Нет записей по турнику." else pullupList.joinToString("\n\n") {
                                "${it.date.format(formatter)}\nВсего подтягиваний: ${it.totalPullUps}\nМаксимум за подход: ${it.maxPullUpsInSet}"
                            }
                            val absReport = if (absList.isEmpty()) "Нет записей по прессу." else "Даты: " + absList.joinToString(", ") { it.date.format(formatter) } + "\nВсего: ${absList.size}"
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "🏊 Бассейн:\n$swimReport\n\n🏋️ Турник:\n$pullupReport\n\n🦾 Пресс:\n$absReport",
                                replyMarkup = mainMenu
                            )
                        }
                        text == "❌ Отмена" && state != null -> {
                            dialogState.remove(userId)
                            bot.sendMessage(
                                chatId = fromId(message.chat.id),
                                text = "Действие отменено.",
                                replyMarkup = mainMenu
                            )
                        }
                        state is DialogState.Swim_Distance -> {
                            val distance = text.toIntOrNull()
                            if (distance == null || distance <= 0) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введите положительное число метров.",
                                    replyMarkup = cancelMenu
                                )
                            } else {
                                dialogState[userId] = DialogState.Swim_TotalTime(distance)
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введи общее время (в секундах):",
                                    replyMarkup = cancelMenu
                                )
                            }
                        }
                        state is DialogState.Swim_TotalTime -> {
                            val totalTime = text.toIntOrNull()
                            if (totalTime == null || totalTime <= 0) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введите положительное число секунд.",
                                    replyMarkup = cancelMenu
                                )
                            } else {
                                dialogState[userId] = DialogState.Swim_Paddles(state.distance, totalTime)
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Дистанция с лопатками (м), если не было — напиши 0:",
                                    replyMarkup = cancelMenu
                                )
                            }
                        }
                        state is DialogState.Swim_Paddles -> {
                            val paddles = text.toIntOrNull()
                            if (paddles == null || paddles < 0) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введите 0 или положительное число.",
                                    replyMarkup = cancelMenu
                                )
                            } else {
                                dialogState[userId] = DialogState.Swim_Best50(state.distance, state.totalTime, if (paddles == 0) null else paddles)
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Лучшее время 50м (сек), если не замеряли — напиши 0:",
                                    replyMarkup = cancelMenu
                                )
                            }
                        }
                        state is DialogState.Swim_Best50 -> {
                            val best50 = text.toIntOrNull()
                            if (best50 == null || best50 < 0) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введите 0 или положительное число.",
                                    replyMarkup = cancelMenu
                                )
                            } else {
                                val user = userRepo.getOrCreateByTelegramId(userId)
                                swimmingRepo.addSwimmingWorkout(
                                    user.id,
                                    state.distance,
                                    state.totalTime,
                                    state.paddlesDistance,
                                    if (best50 == 0) null else best50,
                                    LocalDateTime.now()
                                )
                                dialogState.remove(userId)
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Тренировка по бассейну сохранена!",
                                    replyMarkup = mainMenu
                                )
                            }
                        }
                        state is DialogState.PullUp_Total -> {
                            val total = text.toIntOrNull()
                            if (total == null || total <= 0) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введите положительное число.",
                                    replyMarkup = cancelMenu
                                )
                            } else {
                                dialogState[userId] = DialogState.PullUp_Max(total)
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Максимум за один подход:",
                                    replyMarkup = cancelMenu
                                )
                            }
                        }
                        state is DialogState.PullUp_Max -> {
                            val max = text.toIntOrNull()
                            if (max == null || max <= 0 || max > state.total) {
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Введите положительное число, не больше общего количества.",
                                    replyMarkup = cancelMenu
                                )
                            } else {
                                val user = userRepo.getOrCreateByTelegramId(userId)
                                pullupRepo.addPullUpWorkout(
                                    user.id,
                                    state.total,
                                    max,
                                    LocalDateTime.now()
                                )
                                dialogState.remove(userId)
                                bot.sendMessage(
                                    chatId = fromId(message.chat.id),
                                    text = "Тренировка по турнику сохранена!",
                                    replyMarkup = mainMenu
                                )
                            }
                        }
                        else -> {
                            // Не отвечаем на произвольный текст вне сценария
                        }
                    }
                }
                // Оставляем только команду /start для совместимости
                command("start") {
                    bot.sendMessage(
                        chatId = fromId(message.chat.id),
                        text = "Привет! Я помогу отслеживать твои тренировки. Выбери действие из меню.",
                        replyMarkup = mainMenu
                    )
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
    }.start(wait = true)
}

sealed class DialogState {
    // Бассейн
    data object Swim_Distance : DialogState()
    data class Swim_TotalTime(val distance: Int) : DialogState()
    data class Swim_Paddles(val distance: Int, val totalTime: Int) : DialogState()
    data class Swim_Best50(val distance: Int, val totalTime: Int, val paddlesDistance: Int?) : DialogState()
    // Турник
    data object PullUp_Total : DialogState()
    data class PullUp_Max(val total: Int) : DialogState()
    data object Abs_Add : DialogState()
} 