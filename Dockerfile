# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp verify

FROM eclipse-temurin:25-jre-jammy
RUN groupadd --system --gid 10001 boveda \
    && useradd --system --uid 10001 --gid boveda --home-dir /app boveda \
    && (getent group 1000 >/dev/null || groupadd --gid 1000 render) \
    && usermod -a -G 1000 boveda
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/app.jar ./app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError -XX:ActiveProcessorCount=2"
# Render secret files are readable by group 1000. The named non-root user keeps that supplementary group.
USER boveda
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
