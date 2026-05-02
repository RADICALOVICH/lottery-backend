# lottery-backend

Backend хакатоновой лотереи: регистрация пользователей, админ создаёт тиражи с заранее сгенерированными билетами, пользователи их покупают, шедулер выбирает одного победителя.

Это README — точка входа для разработчика. Подробности архитектуры см. в коде и в будущих файлах из `docs/`.

---
## Порты
- **8080** - приложение.
- **8081** - Swagger.
- **8082** - тесты.

## Стек

- **Java 17** (toolchain в Gradle).
- **Gradle 8.8** (Groovy DSL) + Shadow → fat-jar. Работаем **только через `./gradlew`**, системный `gradle` может быть другой версии.
- **Javalin 6.7** — HTTP. **Без Spring / Spring Boot.**
- **Jackson 2.17** — JSON.
- **PostgreSQL 16** + нативные ENUM-типы (`user_role`, `draw_status`, `ticket_status`).
- **HikariCP 5.1** — пул соединений.
- **Flyway 10.17** — миграции, накатываются из кода при старте (`DatabaseConfig.init`).
- **BCrypt** (`at.favre.lib:bcrypt`) для паролей, cost=12.
- **JDBC-only, без ORM.** Все запросы — руками через `JdbcHelper`.
- **SLF4J 2 + Logback 1.5** — логи.
- Тесты: JUnit 5, AssertJ, Mockito, Testcontainers, REST Assured.

---

## Требования

- JDK 17 (или новее, но toolchain фиксирует 17).
- Docker + Docker Compose — для БД и полного запуска.

Ничего больше ставить не нужно: Gradle подтянется через wrapper, зависимости — через Gradle.

---

## Структура проекта

Package-by-feature: каждая фича (`user`, `draw`, `ticket`) лежит в своём пакете со всеми слоями — repo, service, controller, dto.

```
src/main/java/com/team/lottery/
├── Application.java                ← main: конфиг → БД → Javalin → роуты
├── Wiring.java                     ← Composition Root: ручной DI (пока НЕ написан)
├── config/
│   ├── AppConfig.java              ← чтение application.properties + ENV
│   ├── DatabaseConfig.java         ← Hikari + Flyway.migrate()
│   └── JavalinConfig.java          ← Javalin + Jackson + ErrorHandler
├── common/
│   ├── db/JdbcHelper.java          ← withConnection / withTx / query / update
│   ├── errors/                     ← ApiException + 5 наследников + ErrorHandler + ErrorResponse
│   └── validation/Validators.java  ← notBlank / minLen / maxLen / positive
│
├── user/                           ← фича: регистрация пользователей, роли
│   ├── dto/
│   │   ├── RegisterRequest.java    ← вход POST /auth/register
│   │   └── UserDto.java            ← выход API (без password_hash!)
│   ├── Role.java                   ← enum (USER, ADMIN) — маппится на PG user_role
│   ├── User.java                   ← доменная запись (record) — строка таблицы users
│   ├── UserRepository.java         ← ИНТЕРФЕЙС: findByLogin, save, findById
│   ├── UserJdbcRepository.java     ← РЕАЛИЗАЦИЯ на JDBC (эталон для остальных)
│   ├── UserService.java            ← бизнес-логика: валидация, BCrypt, register
│   └── UserController.java         ← HTTP-ручки /auth/register
│
├── draw/                           ← фича: тиражи, выбор победителя
│   ├── dto/
│   │   ├── CreateDrawRequest.java  ← вход POST /draws (title, totalTickets, endDate)
│   │   ├── DrawDto.java            ← выход API
│   │   └── DrawResultDto.java      ← победитель тиража
│   ├── DrawStatus.java             ← enum (ACTIVE, CLOSED, COMPLETED)
│   ├── Draw.java                   ← доменная запись
│   ├── DrawResult.java             ← запись из таблицы draw_results
│   ├── DrawRepository.java         ← ИНТЕРФЕЙС
│   ├── DrawJdbcRepository.java     ← РЕАЛИЗАЦИЯ
│   ├── DrawResultRepository.java   ← ИНТЕРФЕЙС
│   ├── DrawResultJdbcRepository.java ← РЕАЛИЗАЦИЯ
│   ├── DrawService.java            ← создание draw + pre-generate билетов, закрытие, выбор победителя
│   ├── DrawController.java         ← HTTP-ручки /draws/*
│   └── DrawScheduler.java          ← фоновый розыгрыш по end_date
│
└── ticket/                         ← фича: билеты, покупка
    ├── dto/
    │   ├── TicketDto.java
    │   └── BuyTicketResponse.java
    ├── TicketStatus.java           ← enum (AVAILABLE, SOLD, WIN, LOSE)
    ├── Ticket.java                 ← доменная запись
    ├── TicketRepository.java       ← ИНТЕРФЕЙС
    ├── TicketJdbcRepository.java   ← РЕАЛИЗАЦИЯ
    ├── TicketService.java          ← атомарная покупка AVAILABLE → SOLD
    └── TicketController.java       ← HTTP-ручки /tickets/*

src/main/resources/
├── application.properties          ← дефолты, переопределяются ENV
├── logback.xml
└── db/migration/                   ← Flyway: V1__init_schema.sql, V2__seed_admin.sql

src/test/java/com/team/lottery/
├── support/                        ← Testcontainers-база
├── user/ draw/ ticket/             ← юнит- и интеграционные тесты по фичам
├── common/validation/
└── smoke/                          ← e2e-смоук через REST Assured
```

**Правила организации и именования:**

- Кладём файл в папку **фичи**, к которой он относится, а не в `repository/` / `service/` / `controller/`.
- Общий код (исключения, валидация, JDBC-обёртка) — в `common/`.
- **Интерфейс репозитория** — `<Сущность>Repository` (например, `UserRepository`).
- **Реализация** — `<Сущность><Технология>Repository` (например, `UserJdbcRepository`). Суффикс = технология. Если завтра появится `InMemoryUserRepository` для тестов — она ляжет рядом без переписывания сервисов.
- Сервис и контроллер — по одному на фичу (`UserService`, `UserController`).
- `Wiring.java` — **единственное место**, где собираются зависимости. Никаких аннотаций, никакого сканирования.

---

## Скелет слоёв: что лежит в каждом файле (на примере user)

Минимальный каркас — по одному методу/полю на каждый слой, чтобы было видно назначение файла. Полный `register` со всеми проверками и примером запроса — в разделе «Пример API-ручки» ниже.

```java
// user/Role.java — enum ролей, маппится на PG user_role
public enum Role { USER, ADMIN }
```

```java
// user/User.java — доменная запись (1-в-1 строка таблицы users)
public record User(
        long id,
        String login,
        String passwordHash,
        Role role,
        Instant createdAt
) {}
```

```java
// user/dto/RegisterRequest.java — вход
public record RegisterRequest(String login, String password) {}

// user/dto/UserDto.java — выход (password_hash НЕ отдаём)
public record UserDto(long id, String login, String role, Instant createdAt) {
    public static UserDto from(User u) {
        return new UserDto(u.id(), u.login(), u.role().name(), u.createdAt());
    }
}
```

```java
// user/UserRepository.java — ИНТЕРФЕЙС: контракт для сервиса
public interface UserRepository {
    Optional<User> findByLogin(String login);
    User save(String login, String passwordHash, Role role);
    Optional<User> findById(long id);
}
```

```java
// user/UserJdbcRepository.java — РЕАЛИЗАЦИЯ через JdbcHelper
public class UserJdbcRepository implements UserRepository {

    private final DataSource ds;

    public UserJdbcRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return JdbcHelper.withConnection(ds, c -> {
            List<User> rows = JdbcHelper.query(
                    c,
                    "SELECT id, login, password_hash, role::text, created_at " +
                    "FROM users WHERE login = ?",
                    ps -> ps.setString(1, login),
                    rs -> new User(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("password_hash"),
                            Role.valueOf(rs.getString("role")),
                            rs.getTimestamp("created_at").toInstant()
                    )
            );
            return rows.stream().findFirst();
        });
    }

    // save(...) и findById(...) — по тому же паттерну
}
```

```java
// user/UserService.java — бизнес-логика, не знает про HTTP
public class UserService {

    private final UserRepository userRepo;
    private final AppConfig cfg;

    public UserService(UserRepository userRepo, AppConfig cfg) {
        this.userRepo = userRepo;
        this.cfg = cfg;
    }

    public UserDto register(RegisterRequest req) {
        // валидация через Validators, проверка конфликта,
        // BCrypt.hash, userRepo.save, UserDto.from — см. «Пример API-ручки»
    }
}
```

```java
// user/UserController.java — тонкий HTTP-слой
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    public void register(Context ctx) {
        RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
        UserDto dto = service.register(req);
        ctx.status(201).json(dto);
    }
}
```

Для `draw/` и `ticket/` — **та же схема**: enum статуса, domain record (+ `DrawResult` для draw), Repository (интерфейс + JDBC), Service, Controller, DTO. Отличается только содержимое методов.

---

## Как запускать

### Вариант 1. Локально (рекомендуется для разработки)

База поднимается в контейнере, приложение — с хоста через Gradle. Это быстрее перезапускается при изменениях кода.

```bash
# 1. Поднять только БД
docker compose up -d db

# 2. Запустить приложение (БД по localhost:5433)
./gradlew run
```

Приложение слушает `http://localhost:8080`. Health-check: `GET /health`.

> **Почему 5433, а не 5432?** На многих машинах 5432 уже занят локально установленным Postgres (Postgres.app, brew, системный демон). Чтобы не конфликтовать, контейнер БД маппится на хостовый порт **5433**. Внутри docker compose (вариант 2) контейнер `app` ходит к `db:5432` — это внутренний порт compose-сети, он не меняется.

### Вариант 2. Полностью в docker compose

Для проверки продовой сборки (shadow-jar внутри контейнера):

```bash
docker compose --profile full up --build
```

Профиль `full` включает контейнер приложения; без него поднимается только БД.

### Полезные команды

```bash
./gradlew build      # компиляция + тесты + shadowJar
./gradlew run        # запуск приложения
./gradlew shadowJar  # fat-jar в build/libs/lottery-backend.jar
```

---

## Переменные окружения

Все параметры имеют дефолты в `src/main/resources/application.properties` и перекрываются переменными окружения. Для локального запуска достаточно дефолтов. Для docker compose заведён `.env.example` — скопируй его в `.env`:

```bash
cp .env.example .env
```

| ENV                                | Назначение                        | Дефолт                                     |
|------------------------------------|-----------------------------------|--------------------------------------------|
| `APP_PORT`                         | Порт HTTP-сервера                 | `8080`                                     |
| `DB_URL`                           | JDBC URL Postgres                 | `jdbc:postgresql://localhost:5433/lottery` |
| `DB_USER`                          | Пользователь БД                   | `lottery`                                  |
| `DB_PASSWORD`                      | Пароль БД                         | `lottery_dev_password`                     |
| `DB_POOL_SIZE`                     | Размер пула Hikari                | `10`                                       |
| `BCRYPT_COST`                      | Стоимость хэширования паролей     | `12`                                       |
| `DRAW_SCHEDULER_INTERVAL_SECONDS`  | Период опроса просроченных draw'ов| `60`                                       |

---

## Валидация входов

Используем статические хелперы из `common/validation/Validators.java`. Каждый кидает `ValidationException` → HTTP 400 с телом `{ "code": "VALIDATION_FAILED", "message": "..." }`.

Пример из `UserService.register`:

```java
public User register(RegisterRequest req) {
    Validators.notBlank(req.login(), "login");
    Validators.minLen(req.login(), 3, "login");
    Validators.maxLen(req.login(), 64, "login");
    Validators.notBlank(req.password(), "password");
    Validators.minLen(req.password(), 8, "password");
    // ... дальше логика регистрации
}
```

**Правила:**

- Валидируем форму запроса в сервисе, **до** обращения в репозиторий.
- Бизнес-конфликты (login занят, билет уже продан, draw закрыт) — это не `ValidationException`, а `ConflictException` → 409.
- Для своих специфичных проверок кидаем исключения напрямую: `throw new ConflictException("login already taken");`.

Доступные исключения в `common/errors/`: `ValidationException` (400), `UnauthorizedException` (401), `ForbiddenException` (403), `NotFoundException` (404), `ConflictException` (409). Всё, что не ApiException, ErrorHandler превратит в 500 и залогирует стек.

---

## Работа с БД через JdbcHelper

В проекте нет ORM. Все запросы — `PreparedStatement` с параметрами. Чтобы не повторять try-with-resources и управление транзакциями, используем `common/db/JdbcHelper.java`.

Четыре метода:

| Метод                              | Когда                                           |
|------------------------------------|-------------------------------------------------|
| `withConnection(ds, work)`         | одно чтение/запись, транзакция не нужна         |
| `withTx(ds, work)`                 | несколько операций должны быть атомарны         |
| `query(c, sql, setter, mapper)`    | `SELECT` → `List<T>`                            |
| `update(c, sql, setter)`           | `UPDATE`/`DELETE`/`INSERT` без RETURNING        |

### Пример: простое чтение

```java
public Optional<User> findByLogin(String login) {
    return JdbcHelper.withConnection(ds, c -> {
        List<User> rows = JdbcHelper.query(
                c,
                "SELECT id, login, password_hash, role::text, created_at " +
                "FROM users WHERE login = ?",
                ps -> ps.setString(1, login),
                rs -> new User(
                        rs.getLong("id"),
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role")),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
        return rows.stream().findFirst();
    });
}
```

### Пример: INSERT с RETURNING id

PG ENUM-колонки требуют явного каста `?::user_role` — JDBC не знает о пользовательских типах.

```java
public User save(String login, String passwordHash, Role role) {
    return JdbcHelper.withConnection(ds, c -> {
        List<User> inserted = JdbcHelper.query(
                c,
                "INSERT INTO users (login, password_hash, role) " +
                "VALUES (?, ?, ?::user_role) " +
                "RETURNING id, login, password_hash, role::text, created_at",
                ps -> {
                    ps.setString(1, login);
                    ps.setString(2, passwordHash);
                    ps.setString(3, role.name());
                },
                rs -> new User(
                        rs.getLong("id"),
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role")),
                        rs.getTimestamp("created_at").toInstant()
                )
        );
        return inserted.get(0);
    });
}
```

### Пример: транзакция с несколькими шагами

Создание draw'а: одной транзакцией вставляем draw и N билетов.

```java
public Draw createDraw(CreateDrawRequest req) {
    return JdbcHelper.withTx(ds, c -> {
        // 1. INSERT draw, получаем id
        List<Long> ids = JdbcHelper.query(
                c,
                "INSERT INTO draws (title, total_tickets, deadline_at, status) " +
                "VALUES (?, ?, ?, 'ACTIVE'::draw_status) RETURNING id",
                ps -> {
                    ps.setString(1, req.title());
                    ps.setInt(2, req.totalTickets());
                    ps.setTimestamp(3, Timestamp.from(req.deadlineAt()));
                },
                rs -> rs.getLong(1)
        );
        long drawId = ids.get(0);

        // 2. Генерируем N билетов в статусе AVAILABLE
        JdbcHelper.update(
                c,
                "INSERT INTO tickets (draw_id, ticket_number, status) " +
                "SELECT ?, gs, 'AVAILABLE'::ticket_status " +
                "FROM generate_series(1, ?) AS gs",
                ps -> {
                    ps.setLong(1, drawId);
                    ps.setInt(2, req.totalTickets());
                }
        );

        return drawRepository.findById(drawId, c).orElseThrow();
    });
}
```

Если в `work` бросится исключение — `withTx` сделает `rollback` и пробросит оригинал. `ApiException` долетит до `ErrorHandler` с нужным HTTP-кодом, прочее станет 500.

**Правила:**

- PG ENUM → каст в SQL: `?::user_role`, `?::draw_status`, `?::ticket_status`. В Java передаём `role.name()`.
- Обратное чтение — через `role::text` в SELECT, затем `Role.valueOf(rs.getString("role"))`.
- Не ловим `SQLException` руками внутри `work` — `JdbcHelper` обернёт в `RuntimeException`, а `ErrorHandler` отдаст 500 и залогирует стек.
- Для атомарности — только `withTx`. Не пытайтесь вручную дёргать `setAutoCommit`/`commit`/`rollback`.

---

## Пример API-ручки: DTO → Service → Controller

Чтобы добавить новый эндпоинт, нужно четыре кусочка: DTO на вход, DTO на выход, метод сервиса, метод контроллера, и регистрация роута. Ниже — пример для `POST /auth/register`. Его же структура работает для `POST /draws`, `POST /tickets/{id}/buy` и всех остальных ручек.

### 1. DTO

Records — Jackson сериализует/десериализует без аннотаций.

```java
// user/dto/RegisterRequest.java
public record RegisterRequest(String login, String password) {}

// user/dto/UserDto.java
public record UserDto(long id, String login, String role, Instant createdAt) {
    public static UserDto from(User u) {
        return new UserDto(u.id(), u.login(), u.role().name(), u.createdAt());
    }
}
```

Правило: DTO живут в `<feature>/dto/`, доменные классы (`User`, `Draw`, `Ticket`) — в корне фичи.

### 2. Метод сервиса

Сервис принимает входной DTO, валидирует, делает бизнес-логику, возвращает выходной DTO. **Исключения не ловим** — `ErrorHandler` сам превратит `ApiException` в нужный HTTP-код.

```java
// user/UserService.java
public UserDto register(RegisterRequest req) {
    Validators.notBlank(req.login(), "login");
    Validators.minLen(req.login(), 3, "login");
    Validators.notBlank(req.password(), "password");
    Validators.minLen(req.password(), 8, "password");

    if (userRepo.findByLogin(req.login()).isPresent()) {
        throw new ConflictException("login already taken");
    }

    String hash = BCrypt.withDefaults()
            .hashToString(cfg.bcryptCost(), req.password().toCharArray());
    User user = userRepo.save(req.login(), hash, Role.USER);
    return UserDto.from(user);
}
```

### 3. Controller

Тонкий слой между HTTP и сервисом: парсит тело, вызывает сервис, выставляет статус. Никакой бизнес-логики.

```java
// user/UserController.java
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    public void register(Context ctx) {
        RegisterRequest req = ctx.bodyAsClass(RegisterRequest.class);
        UserDto dto = service.register(req);
        ctx.status(201).json(dto);
    }
}
```

**Правила контроллера:**

- Никакой бизнес-логики — только парсинг входа, вызов сервиса, ответ.
- Не ловим исключения — `ErrorHandler` поймает `ApiException` и вернёт нужный код.
- Path-параметры: `long id = ctx.pathParamAsClass("id", Long.class).get();`
- Query-параметры: `int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);`

### 4. Регистрация роута

Пока `Wiring` не написан — сборку графа и регистрацию роутов делаем прямо в `Application.main`:

```java
UserRepository userRepo = new UserJdbcRepository(ds);
UserService userService = new UserService(userRepo, cfg);
UserController userController = new UserController(userService);

app.post("/auth/register", userController::register);
```

Когда появится `Wiring.java` (Composition Root), вся эта сборка переедет в него — в `Application.main` останется `Wiring.assemble(ds, cfg).registerRoutes(app)`.

### Как это выглядит в работе

```bash
curl -X POST http://localhost:8080/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"login":"alice","password":"supersecret"}'
# 201 Created
# {"id":2,"login":"alice","role":"USER","createdAt":"2026-04-19T10:00:00Z"}
```

Возможные ошибки:

| Вход                            | Ответ                                                          |
|---------------------------------|----------------------------------------------------------------|
| `{"login":""}`                  | 400 `{"code":"VALIDATION_FAILED","message":"login must not be blank"}` |
| login уже существует            | 409 `{"code":"CONFLICT","message":"login already taken"}`     |
| JSON невалиден                  | 400 (обработано Javalin автоматически)                         |

---

## Миграции БД

Flyway накатывает всё из `src/main/resources/db/migration/` при старте приложения (`DatabaseConfig.init`).

Именование: `V<N>__<snake_case_description>.sql`, где `N` — монотонно возрастающий номер.

Уже есть:

- `V1__init_schema.sql` — 3 ENUM-типа, 4 таблицы (`users`, `draws`, `tickets`, `draw_results`), индексы, CHECK-констрейнты.
- `V2__seed_admin.sql` — seed-админ `admin / admin123` (BCrypt cost=12).

**Правила:**

- Миграции **только вперёд**. Откат — новой миграцией, не редактированием существующей.
- Опубликованную миграцию не меняем: Flyway посчитает checksum и откажется стартовать.
- Для новой сущности — новая миграция. Менять существующую таблицу — тоже миграцией (`ALTER TABLE`).

---

## Тесты

Для корректной работы тестов необходимо обеспечить права: команда docker ps должна выполняться без sudo.
Для этого:


```bash
sudo groupadd docker
```

```bash
sudo usermod -aG docker $USER
```

Применить эти изменения к текущие изменения к текущей терминальной сессии:
```bash
newgrp docker
```

### Стандартный отчет Gradle (Результаты тестов)

```bash
./gradlew test
```
Или:

```bash
./gradlew test --rerun
```

**Где искать отчет:**
build/reports/tests/test/index.html

**Что там есть:**
Список всех пройденных/упавших тестов, время выполнения и логи ошибок.


### Отчет JaCoCo (Покрытие кода)

Этот инструмент показывает, какой процент вашего кода (строки, ветвления) был реально затронут тестами. У вас он уже настроен в блоке tasks.jacocoTestReport.
Как запустить:
```bash
./gradlew test jacocoTestReport
```


**Где искать отчет:**
build/reports/jacoco/test/html/index.html

**Что там есть:** Цветная разметка кода (зеленое — протестировано, красное — нет) и статистика по пакетам/классам.


- **Unit-тесты** (`*Test`) — логика сервисов с моками репозиториев, валидации.
- **Integration-тесты** (`*IT`) — поднимают Postgres через Testcontainers и бьют по настоящей БД. Docker должен быть запущен.
- **Smoke** — полный старт Javalin + дёрганье `/health` через REST Assured.

Эталонные тесты, на которые смотрим при написании новых:

- `user/UserServiceTest.java` — unit.
- `user/UserJdbcRepositoryIT.java` — integration.

---

## Конвенции

- **Комментарии в коде — на русском, идентификаторы — на английском.** Сообщения API-ошибок — на английском (уходят клиенту).
- **Без ORM, без аннотаций.** Зависимости собираются руками в `Wiring.java`.
- **PG ENUM** передаём строкой с кастом `?::<enum>` в SQL, читаем через `::text`.
- **BIGSERIAL `id`**, не UUID.
- **BCrypt cost=12** (из ENV `BCRYPT_COST`).
- **Seed-админ**: `login=admin`, `password=admin123`. Менять пароль админа — только через новую миграцию или отдельный эндпоинт, не переписыванием V2.
- **Pre-generated tickets**: при создании draw'а сразу INSERT'им N билетов в статусе `AVAILABLE`. Покупка — атомарный переход `AVAILABLE → SOLD` (с проставлением `owner_id`). Эту атомарность обеспечивает `UPDATE ... WHERE status = 'AVAILABLE'` + проверка `rowsAffected == 1`.
- **Один победитель на тираж** → отдельная таблица `draw_results` с `UNIQUE(draw_id)`.

---

## Health-check

`GET /health` → `200 OK` с минимальным JSON:

```json
{ "status": "ok" }
```

Используется Docker-healthcheck'ом в `Dockerfile` и может быть использован мониторингом.


## Swagger

Для корректной работы Swagger в resources/application.properties необходимо задать 

```
app.isProd=false
```

Иначе говоря, документация Swagger корректно заработает в любой среде кроме продуктивной (в среде
разработки и в тестовой среде).


Как пользоваться


- **Вытянуть готовый образ swagger-ui:**

```
sudo docker pull docker.swagger.io/swaggerapi/swagger-ui
```


- **Запустить проект на порту 8080.**

- **В терминале перейти в каталог, где располагается spring.yaml.**

Пример (от корневого каталога проекта):

```
cd swagger
```

- **Запустить сервер Swagger на порту 8081 (чтобы не конфликтовал за порт с проектом).**

```
sudo docker run --rm -p 8081:8080 \
  -e SWAGGER_JSON=/spec/swagger.yaml \
  -v "$PWD":/spec \
  --name swaggerui swaggerapi/swagger-ui
```

- **В браузере http://localhost:8081/**

- **Нажимать Try it out / Execute.** 

