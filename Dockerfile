# ---- Build stage ----
FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :bot:installDist --no-daemon

# ---- Run stage ----
FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/bot/build/install/bot/ ./
# Даем права на исполнение скрипту
RUN chmod +x /app/bin/bot
EXPOSE 8080
ENV PORT=8080
# Запускаем напрямую наш собственный main class, а не EngineMain от Ktor
CMD ["java", "-classpath", "lib/*", "bot.BotApplicationKt"]