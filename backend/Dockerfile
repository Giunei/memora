# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src

COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package \
    && mv target/memora-backend-*.jar /tmp/app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system memora \
    && useradd --system --gid memora --home-dir /app --shell /usr/sbin/nologin memora

COPY --from=build /tmp/app.jar /app/app.jar
USER memora

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=12 \
    CMD curl -fsS http://localhost:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
