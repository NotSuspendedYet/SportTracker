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
# Сначала выводим структуру файлов для дебага, потом запускаем
CMD ["/bin/sh", "-c", "echo 'Listing /app:'; ls -l /app; echo 'Listing /app/bin:'; ls -l /app/bin; echo 'Listing /app/lib:'; ls -l /app/lib; echo 'Starting bot...'; /app/bin/bot"]