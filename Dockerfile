# ---- Build stage ----
FROM gradle:8.8-jdk17 AS build
WORKDIR /app

# Сначала копируем только то, что нужно для скачивания зависимостей (кэш слоёв)
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN gradle --no-daemon dependencies || true

# Потом уже исходники
COPY src ./src
RUN gradle --no-daemon shadowJar -x test

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Не root
RUN groupadd -r app && useradd -r -g app app
USER app

COPY --from=build /app/build/libs/lottery-backend-all.jar ./app.jar

EXPOSE 8080

# Health check на уровне контейнера
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
