# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew clean bootJar --no-daemon && \
    JAR_FILE="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)" && \
    test -n "$JAR_FILE" && \
    cp "$JAR_FILE" /workspace/app.jar


FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/app.jar ./app.jar

RUN mkdir -p /app/data/uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]