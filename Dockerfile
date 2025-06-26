# ---- Build stage ----
FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle :bot:installDist --no-daemon

# ---- Run stage ----
FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/bot/build/install/bot/ ./
EXPOSE 8080
ENV PORT=8080
CMD ["bin/bot"] 