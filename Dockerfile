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
# Запускаем напрямую через java, так как скрипт может работать некорректно
CMD ["java", "-classpath", "lib/*", "io.ktor.server.netty.EngineMain"]