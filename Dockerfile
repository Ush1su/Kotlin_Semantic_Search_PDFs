# syntax=docker/dockerfile:1

# The React app is built in its own stage so the JDK stage never needs npm.
FROM node:22-alpine AS frontend

WORKDIR /frontend

COPY frontend/package.json frontend/package-lock.json ./

RUN npm ci

COPY frontend/ ./

RUN npm run build


FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

COPY src ./src

COPY --from=frontend /frontend/dist ./src/main/resources/static

RUN ./gradlew clean bootJar --no-daemon -PskipFrontend && \
    JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" && \
    test -n "$JAR_FILE" && \
    cp "$JAR_FILE" /workspace/app.jar


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/app.jar ./app.jar

RUN mkdir -p /app/data/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
