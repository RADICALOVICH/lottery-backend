# Draws — текущий checkpoint

## Что уже сделано
- `POST /admin/draws` работает
- `GET /api/v1/draws` работает
- фильтрация через `status` работает:
  - `GET /api/v1/draws?status=ACTIVE`
  - `GET /api/v1/draws?status=COMPLETED`
- `GET /api/v1/draws/{id}` работает
- `POST /admin/draws/{id}/run-draw` есть
- `GET /api/v1/draws/{id}/result` есть
- базовая валидация create draw работает
- draw-контроллеры адаптированы под новый способ регистрации роутов
- `Application` и `JavalinConfig` локально согласованы с draw-блоком

## Что уже есть по бизнес-логике
- `runDraw(...)` ищет тираж по `id`
- если тираж не найден → `NotFoundException`
- если статус не `CLOSED` → `ConflictException`
- при успешном сценарии тираж переводится в `COMPLETED`
- создается `DrawResult`
- результат сохраняется через `DrawResultRepository`
- результат читается через `getDrawResultByDrawId(...)`

## Что уже есть по моделям/репозиториям
- `Draw`
- `DrawStatus`
- `DrawResult`
- `DrawResultResponse`
- `DrawRepository`
- `InMemoryDrawRepository`
- `DrawResultRepository`
- `InMemoryDrawResultRepository`
- `JdbcDrawResultRepository` создан как подготовка под JDBC

## Что уже есть по auth/integration
- в `AdminDrawController` добавлен `requireAdminId(ctx)` как точка будущей интеграции с реальной аутентификацией
- пока это временная заглушка с фиксированным `adminId`
- и `POST /admin/draws`, и `POST /admin/draws/{id}/run-draw` уже проходят через эту точку

## Что уже проверено локально
- проект собирается
- сервер стартует
- create draw работает
- список draw работает
- draw by id работает
- result endpoint работает
- `DrawResult` сохраняется и читается
- `GET /api/v1/draws/{id}/result` отдает JSON

## Что еще не сделано
- выбор победителя из ticket-блока
- заполнение `winningTicketId`
- массовая простановка `LOSE` всем SOLD билетам
- проставка `WIN` победителю
- в `runDraw(...)` зафиксирован TODO под ticket-интеграцию: проверка SOLD-билетов, обработка несостоявшегося тиража, выбор `winningTicketId` и простановка `WIN/LOSE`
- JDBC/PostgreSQL подключение `DrawResultRepository` в рабочий поток
- JDBC/PostgreSQL реализация для draw-блока целиком
- scheduler
- поиск тиражей, готовых к автообработке
- защита от повторного/параллельного запуска одного и того же draw
- финальная интеграция с ticket-блоком

## Важно держать в памяти
- текущая логика `draw_results` пока промежуточная: результат сохраняется без `winningTicketId`, так как ticket-блок еще не подключен
- `JdbcDrawResultRepository` уже создан и компилируется, но пока не подключается в рабочий поток
- причина: в БД `winning_ticket_id` объявлен как `NOT NULL`, а ticket-блок еще не подключен и `winningTicketId` пока не заполняется
- в общий `main` изменения по `Application` / `JavalinConfig` нельзя вливать отдельно без draw-блока целиком
- в `runDraw(...)` уже отмечена точка будущей интеграции с ticket-блоком, чтобы не потерять следующую доработку

## Последний зафиксированный коммит
- `feat(draws): add draw result flow and prepare auth/jdbc integration`