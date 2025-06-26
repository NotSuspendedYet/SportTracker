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
- Start Command: не нужен (используется Dockerfile)
- Environment → Add Environment Variables:
  - `BOT_TOKEN` — токен Telegram-бота
  - `DATABASE_URL` — строка подключения к БД (internal или external, формат: `postgresql://user:password@host:port/db`)
  - `PORT` — `8080` (Render автоматически пробрасывает порт)
- Region: Europe или ближайший к вам
- Dockerfile используется для сборки и запуска

### 3. Настройте Telegram Webhook (опционально)
- Render выдаст вам публичный URL (например, `https://sporttracker.onrender.com`)
- Можно использовать polling (по умолчанию), либо настроить webhook через BotFather:
  - Для polling: удалите webhook (см. FAQ ниже)
  - Для webhook: реализуйте endpoint и пропишите его через `/setwebhook`

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
docker run -e BOT_TOKEN=... -e DATABASE_URL=... -p 8080:8080 sporttracker
```

## Переменные окружения
- `BOT_TOKEN` — токен Telegram-бота
- `DATABASE_URL` — строка подключения к PostgreSQL (например, postgresql://user:password@host:port/db)
- `PORT` — порт (по умолчанию 8080)

## Миграции
Таблицы создаются автоматически при запуске.

---

## FAQ и типовые ошибки при деплое на Render

### 1. Ошибка: `Can't resolve dialect for connection: postgresql://...`
**Причина:** Render выдаёт DATABASE_URL в формате, который не понимает JDBC/Exposed.
**Решение:** В проекте реализован автоматический парсер DATABASE_URL, который преобразует его в нужный формат. Просто указывайте переменную как есть (internal или external URL Render).

### 2. Ошибка: `Unable to parse URL jdbc:postgresql://user:password@host:port/db`
**Причина:** JDBC требует user и password как параметры, а не в user:password@.
**Решение:** В проекте реализован парсер, который преобразует URL в формат `jdbc:postgresql://host:port/db?user=...&password=...`.

### 3. Ошибка: `Usage: java [options] <mainclass> [args...]`
**Причина:** Скрипт запуска Gradle (`/app/bin/bot`) не всегда работает в Docker/Render.
**Решение:** В Dockerfile запуск производится напрямую через `java -classpath "lib/*" bot.BotApplicationKt`.

### 4. Ошибка: `Module function cannot be found for the fully qualified name ...`
**Причина:** Попытка запускать стандартный Ktor EngineMain вместо своей точки входа.
**Решение:** В Dockerfile явно указывается запуск вашего main-класса: `bot.BotApplicationKt`.

### 5. Ошибка: `409 Conflict: can't use getUpdates method while webhook is active; use deleteWebhook to delete the webhook first`
**Причина:** У бота в Telegram уже настроен webhook, а backend пытается работать в режиме polling.
**Решение:**
- Откройте в браузере:  
  `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/deleteWebhook`
- После этого polling будет работать корректно.

### 6. Ошибка: Exited with status 128
**Причина:** Обычно это отсутствие переменных окружения или проблема с правами на скрипт.
**Решение:**
- Проверьте, что заданы все переменные окружения (`BOT_TOKEN`, `DATABASE_URL`, `PORT`).
- В Dockerfile добавлен `chmod +x /app/bin/bot`, но запуск всё равно производится через java.

### 7. Как посмотреть структуру файлов в контейнере?
**Решение:**
- В Dockerfile можно временно добавить команды `ls -l /app`, `ls -l /app/bin`, `ls -l /app/lib` перед запуском, чтобы увидеть структуру в логах Render.

---

## TODO
- Выбор упражнения по номеру
- Кнопки Telegram вместо текстовых команд
- Хранение состояния диалога в Redis
- Периодические задачи (cron) 