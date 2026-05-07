# lottery-backend

Бэкенд лотерейной системы на Java + Javalin + PostgreSQL.
Учебный проект, кейс «Лотерея».

---

## Реализованный сценарий

**Сценарий 1 — базовая лотерея.** Все обязательные функции:

- Аутентификация пользователей (USER / ADMIN), сессии через токены.
- Создание тиражей администратором (билеты пред-генерируются в статусе `AVAILABLE`).
- Получение списка активных тиражей (`GET /draws?status=ACTIVE`).
- Покупка билетов пользователем (`POST /draws/{id}/tickets`).
- Закрытие продажи билетов автоматически по `end_date` (шедулер раз в 30 секунд).
- Проведение розыгрыша администратором по API (`POST /admin/draws/{id}/run-draw`).
- Получение результата тиража (`GET /draws/{id}/result`) и статусов билетов
  (`WIN` / `LOSE`) в `GET /me/results`.

---

## Стек

- **Java 17** (toolchain в Gradle)
- **Gradle 8.8** + Shadow plugin (fat-jar)
- **Javalin 6.7** — HTTP-сервер. Без Spring / Spring Boot, как требует ТЗ.
- **PostgreSQL 16** + native ENUM-типы (`user_role`, `draw_status`, `ticket_status`)
- **HikariCP 5.1** — пул соединений
- **Flyway 10** — миграции, накатываются из кода при старте
- **Jackson 2.17** — JSON
- **JBCrypt 0.4** — хеширование паролей
- **JDBC-only**, без ORM. Все запросы — `PreparedStatement` руками.
- Тесты: JUnit 5, AssertJ, Mockito, **Testcontainers**, REST Assured

---

## Требования

- JDK 17+
- Docker + Docker Compose

---

## Запуск

```bash
docker compose up -d --build
```

Приложение: `http://localhost:8080`. Health-check: `GET /health`.

В БД при первом старте создаётся администратор:
`login=admin`, `password=admin123` (см. `V2__seed_admin.sql`).

Остановить:
```bash
docker compose down
```

С полным сбросом данных:
```bash
docker compose down -v
```

---

## Переменные окружения

Все параметры имеют дефолты в `src/main/resources/application.properties` и
перекрываются ENV.

| ENV | Назначение | Дефолт |
|---|---|---|
| `APP_PORT` | Порт HTTP | `8080` |
| `DB_URL` | JDBC URL Postgres | `jdbc:postgresql://localhost:5433/lottery` |
| `DB_USER` | Пользователь БД | `lottery` |
| `DB_PASSWORD` | Пароль БД | `lottery_dev_password` |
| `DB_POOL_SIZE` | Размер пула Hikari | `10` |
| `BCRYPT_COST` | Стоимость BCrypt | `12` |
| `DRAW_SCHEDULER_INTERVAL_SECONDS` | Период опроса просроченных draw'ов | `60` |

В `docker-compose.yml` все ENV для контейнера `app` уже проставлены —
менять не обязательно.

---

## Архитектура

Package-by-feature: фичи `users`, `draws`, `ticket` лежат в своих пакетах со
всеми слоями. Общая инфраструктура — в `common/` и `config/`.

```
src/main/java/com/team/lottery/
├── Application.java              ← entry point + composition root (startWith)
├── config/
│   ├── AppConfig.java             ← properties + ENV → record
│   ├── DatabaseConfig.java        ← Hikari + Flyway
│   └── JavalinConfig.java         ← Javalin + Jackson + ErrorHandler
├── common/
│   ├── errors/                    ← ApiException + 5 наследников + ErrorHandler
│   ├── validation/Validators.java ← примитивы (notBlank, minLen, ...)
│   └── health/HealthController.java
├── users/
│   ├── controller/ (AuthController, UserController)
│   ├── service/    (AuthService, TokenService)
│   ├── repository/ (UserRepository интерфейс + UserJdbcRepository)
│   ├── model/      (UserAuthData, UserResponse — records)
│   ├── dto/        (RegisterRequest, LoginRequest — records)
│   ├── validation/AuthValidators.java
│   └── util/PasswordUtil.java
├── draws/
│   ├── controller/ (DrawController, AdminDrawController)
│   ├── service/    (DrawService)
│   ├── repository/ (DrawRepository + DrawJdbcRepository, DrawResultRepository + DrawResultJdbcRepository)
│   ├── model/      (Draw, DrawResult — records; DrawStatus enum)
│   ├── dto/        (CreateDrawRequest, DrawResponse, DrawResultResponse — records)
│   ├── mapper/DrawMapper.java
│   ├── validation/DrawValidators.java
│   └── scheduler/DrawScheduler.java
└── ticket/
    ├── controller/ (TicketController)
    ├── service/    (TicketService)
    ├── repository/ (TicketRepository + TicketJdbcRepository)
    ├── model/      (Ticket — record; TicketStatus enum)
    ├── dto/        (TicketResponse, BuyTicketResponse — records)
    └── mapper/TicketMapper.java
```

**Слои:** `Controller` → `Service` → `Repository`. Контроллеры тонкие
(парсинг + вызов сервиса). Бизнес-логика и валидация — в сервисах.
Репозитории — голый JDBC через интерфейс + JDBC-реализацию.

---

## Модель данных

4 таблицы + 3 PostgreSQL ENUM. Полная схема в
`src/main/resources/db/migration/V1__init_schema.sql`.

```
users         (id, login, password_hash, role[USER|ADMIN], created_at)
draws         (id, title, status[ACTIVE|CLOSED|COMPLETED], end_date, total_tickets,
               created_by → users.id, created_at)
tickets       (id, draw_id → draws.id, owner_id → users.id, ticket_number,
               status[AVAILABLE|SOLD|WIN|LOSE], created_at)
draw_results  (id, draw_id → draws.id UNIQUE, winning_ticket_id → tickets.id, drawn_at)
```

**Жизненный цикл тиража:**
```
ACTIVE   ─(end_date passed, scheduler)─►  CLOSED   ─(admin POST run-draw)─►  COMPLETED
```

- **ACTIVE** — тираж создан администратором. Билеты пред-сгенерированы в
  статусе AVAILABLE, пользователи могут их покупать.
- **CLOSED** — наступила `end_date`, шедулер автоматически закрыл продажу.
  Купить новый билет уже нельзя; тираж ждёт явного запуска розыгрыша.
- **COMPLETED** — администратор провёл розыгрыш через
  `POST /admin/draws/{id}/run-draw`. Победитель определён, статусы билетов
  обновлены, в `draw_results` записан выигрышный билет.

Шедулер только закрывает продажу — розыгрыш всегда инициируется админом
вручную через API. Это даёт админу контроль над моментом проведения
розыгрыша после закрытия (например, разобраться со спорными покупками).

**Жизненный цикл билета:**
```
AVAILABLE  ─(пользователь покупает)─►  SOLD  ─(розыгрыш)─►  WIN | LOSE
                                                  ▲
                                  AVAILABLE  ─────┘ (если победил непроданный — статус WIN с null owner_id)
```

- **AVAILABLE** — билет сгенерирован вместе с тиражом, никем не куплен.
- **SOLD** — пользователь купил билет (атомарный переход
  `AVAILABLE → SOLD` с проставлением `owner_id`, защищён `FOR UPDATE SKIP LOCKED`
  от двойной покупки).
- **WIN** — на этот билет выпал выигрыш при розыгрыше.
- **LOSE** — билет был SOLD, но не выиграл; после розыгрыша становится LOSE.

**Розыгрыш проводится среди ВСЕХ билетов тиража, включая непроданные.**
Это сознательное правило лотереи: победителем может стать билет в статусе
AVAILABLE — тогда он переходит в WIN с `owner_id = null`. Запретить
розыгрыш можно только когда **ни один** билет в тираже не куплен (тогда
вернётся 409). Если хотя бы один SOLD есть — розыгрыш идёт, и победитель
выбирается случайно из всего пула.

---

## API

### Публичные

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/health` | health-check |
| `GET` | `/health/db` | проверка БД |
| `POST` | `/auth/register` | регистрация |
| `POST` | `/auth/login` | логин (возвращает токен) |
| `POST` | `/auth/logout` | логаут (требует токен) |
| `GET` | `/draws` | список тиражей (можно `?status=ACTIVE`) |
| `GET` | `/draws/{id}` | тираж по id |
| `GET` | `/draws/{id}/result` | результат тиража (только для COMPLETED) |

### Для USER (требует `Authorization: Bearer <token>`)

| Метод | Путь | Описание |
|---|---|---|
| `GET` | `/users/me` | текущий пользователь |
| `POST` | `/draws/{id}/tickets` | купить билет в тираже |
| `GET` | `/me/tickets` | мои билеты |
| `GET` | `/me/results` | мои билеты со статусом WIN/LOSE |

### Для ADMIN (требует токен админа)

| Метод | Путь | Описание |
|---|---|---|
| `POST` | `/admin/draws` | создать тираж |
| `POST` | `/admin/draws/{id}/run-draw` | провести розыгрыш (только для CLOSED) |
| `GET` | `/users` | список всех пользователей |
| `GET` | `/admin/ping` | проверка прав админа |
| `GET` | `/admin/logged-in-users` | активные сессии |

---

## Примеры запросов

### 1. Логин администратором
```bash
curl -X POST http://localhost:8080/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"login":"admin","password":"admin123"}'
# 200 OK
# {"login":"admin","id":1,"token":"<UUID>","role":"ADMIN","message":"..."}
```

### 2. Регистрация и логин пользователя
```bash
curl -X POST http://localhost:8080/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"supersecret123"}'
# 201 Created — {"id":2,"login":"alice","message":"User registered successfully"}

curl -X POST http://localhost:8080/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"supersecret123"}'
# 200 OK — {"token":"<UUID>","role":"USER",...}
```

### 3. Создание тиража админом
```bash
curl -X POST http://localhost:8080/admin/draws \
     -H 'Authorization: Bearer <ADMIN_TOKEN>' \
     -H 'Content-Type: application/json' \
     -d '{"title":"Holiday Draw","totalTickets":100,"endDate":"2027-12-31T23:59:00+03:00"}'
# 201 Created — {"id":1,"title":"Holiday Draw","status":"ACTIVE","totalTickets":100,...}
```

### 4. Покупка билета пользователем
```bash
curl -X POST http://localhost:8080/draws/1/tickets \
     -H 'Authorization: Bearer <USER_TOKEN>'
# 200 OK
# {"message":"Ticket purchased successfully","ticket":{"id":1,"status":"SOLD","ownerId":2,...}}
```

### 5. Список ACTIVE тиражей
```bash
curl http://localhost:8080/draws?status=ACTIVE
```

### 6. Розыгрыш (после того как шедулер закрыл тираж по end_date)
```bash
curl -X POST http://localhost:8080/admin/draws/1/run-draw \
     -H 'Authorization: Bearer <ADMIN_TOKEN>'
# 200 OK — {"id":1,"status":"COMPLETED",...}
```

### 7. Результат тиража и мои выигрыши
```bash
curl http://localhost:8080/draws/1/result
# {"drawId":1,"winningTicketId":42,"drawnAt":"..."}

curl http://localhost:8080/me/results -H 'Authorization: Bearer <USER_TOKEN>'
# [{"id":1,"status":"LOSE",...}, ...]
```

В `TestApi/` лежат `.http` файлы для прогона из IntelliJ.

---

## Тесты

```bash
./gradlew test         # юнит + интеграционные + смоук
./gradlew build        # + компиляция + shadowJar
```

**Тесты self-contained** — не требуют поднятого `docker compose`. Используют
Testcontainers (singleton-контейнер `TestPostgres` поднимается из тестового
JVM, миграции прогоняются автоматически).

Структура тестов:
- `src/test/.../unit/` — юнит-тесты сервисов с моками, валидаторов, утилит
- `src/test/.../unit/repository/` — JDBC-тесты репозиториев на реальном Postgres
  (Testcontainer)
- `src/test/.../integration/` — HTTP-тесты через REST Assured: запускают
  приложение на порту 8082 и бьют по API (Testcontainer для БД)
- `src/test/.../smoke/ApplicationSmokeTest.java` — самый тонкий smoke

Покрытие отчётом JaCoCo:
```bash
./gradlew test jacocoTestReport
# build/reports/jacoco/test/html/index.html
```

---

## Swagger

OpenAPI-спека в `swagger/swagger.yaml`. Для просмотра в браузере:

```bash
docker run --rm -p 8081:8080 \
  -e SWAGGER_JSON=/spec/swagger.yaml \
  -v "$PWD/swagger":/spec \
  swaggerapi/swagger-ui
```

Открыть `http://localhost:8081/`.
