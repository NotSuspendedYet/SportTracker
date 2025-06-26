# SportTracker Telegram Bot

Бэкенд на Kotlin (Ktor, Exposed, Telegram Bot DSL) для трекинга спортивных тренировок.

---

## 🚀 Быстрый старт на Render

### 1. Создайте PostgreSQL базу данных
- В Render: **Dashboard → New → PostgreSQL**
- Скопируйте параметры подключения (host, db, user, password)

### 2. Разверните Backend
- **Dashboard → New → Web Service**
- Source: ваш GitHub-репозиторий с этим проектом
- Build Command: `./gradlew :bot:installDist`
- Start Command: `./bot/build/install/bot/bin/bot`
- Environment → Add Environment Variables:
  - `BOT_TOKEN` — токен Telegram-бота
  - `DATABASE_URL` — например, `jdbc:postgresql://<host>:5432/<db>`
  - `DATABASE_USER` — пользователь БД
  - `DATABASE_PASSWORD` — пароль БД
  - `PORT` — `8080` (Render автоматически пробрасывает порт)
- Region: Europe или ближайший к вам
- Dockerfile не нужен — Render сам соберёт по gradle

### 3. Настройте Telegram Webhook (опционально)
- Render выдаст вам публичный URL (например, `https://sporttracker.onrender.com`)
- Можно использовать polling (по умолчанию), либо настроить webhook через BotFather:
  - `/setwebhook` → URL: `https://sporttracker.onrender.com` (если реализуете webhook)

---

## 🗂 Карта проекта

```
SportTracker/
├── bot/                # UI-слой, Ktor + Telegram Bot DSL
│   ├── src/main/kotlin/BotApplication.kt   # Точка входа, обработка команд, запуск Ktor
│   └── src/main/resources/
│       ├── application.conf                # Конфиг Ktor
│       └── logback.xml                     # Логирование
│   └── build.gradle.kts                    # Зависимости bot-слоя
├── domain/            # Бизнес-логика, сущности
│   ├── src/main/kotlin/Entities.kt         # data-классы: User, Exercise, Workout, Set
│   └── src/main/kotlin/WorkoutParser.kt    # Парсер ввода подходов (12x3@50, 15x2)
│   └── build.gradle.kts                    # Зависимости domain-слоя
├── data/              # Слой доступа к данным (Exposed, PostgreSQL)
│   ├── src/main/kotlin/DatabaseTables.kt   # Описание таблиц Exposed
│   ├── src/main/kotlin/DatabaseFactory.kt  # Инициализация БД
│   ├── src/main/kotlin/Repositories.kt     # Репозитории users, exercises, workouts
│   ├── src/main/kotlin/SetRepository.kt    # Репозиторий для sets (подходов)
│   └── build.gradle.kts                    # Зависимости data-слоя
├── build.gradle.kts    # Корневой gradle, общие зависимости
├── settings.gradle.kts # Модули проекта
├── gradle.properties   # Настройки gradle
├── .gitignore          # Исключения для git
├── Dockerfile          # Сборка и запуск в Docker/Render
└── README.md           # Документация, инструкция по запуску
```

---

## 📚 Описание структуры и ключевых файлов

- **bot/BotApplication.kt** — основной файл, где:
  - запускается Ktor-сервер
  - инициализируется Telegram-бот
  - реализована логика команд: добавление упражнения, запись тренировки, отчёт, хранение состояния диалога
- **domain/Entities.kt** — data-классы для пользователей, упражнений, тренировок, подходов
- **domain/WorkoutParser.kt** — парсер пользовательского ввода подходов (например, 12x3@50)
- **data/DatabaseTables.kt** — описание таблиц users, exercises, workouts, sets через Exposed ORM
- **data/DatabaseFactory.kt** — подключение к PostgreSQL, создание таблиц
- **data/Repositories.kt, SetRepository.kt** — CRUD-операции для всех сущностей
- **bot/application.conf, logback.xml** — конфигурация Ktor и логирования
- **Dockerfile** — multi-stage build для деплоя на Render или локально через Docker

---

## Основные команды бота
- ➕ Добавить упражнение
- 🏋️ Записать тренировку
- 📊 Отчёт (за неделю, месяц, всё время)
- Уведомления: ежедневные и еженедельные (TODO)

---

## Сборка и запуск локально

```sh
./gradlew :bot:run
```

## Сборка и запуск через Docker

```sh
docker build -t sporttracker .
docker run -e BOT_TOKEN=... -e DATABASE_URL=... -e DATABASE_USER=... -e DATABASE_PASSWORD=... -p 8080:8080 sporttracker
```

## Переменные окружения
- `BOT_TOKEN` — токен Telegram-бота
- `DATABASE_URL` — JDBC-URL PostgreSQL (например, jdbc:postgresql://host:5432/db)
- `DATABASE_USER` — пользователь БД
- `DATABASE_PASSWORD` — пароль БД
- `PORT` — порт (по умолчанию 8080)

## Миграции
Таблицы создаются автоматически при запуске.

---

## TODO
- Выбор упражнения по номеру
- Кнопки Telegram вместо текстовых команд
- Хранение состояния диалога в Redis
- Периодические задачи (cron) 