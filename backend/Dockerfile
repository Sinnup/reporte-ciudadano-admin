FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copy wrapper + config first for layer caching
COPY gradlew gradlew
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./
COPY backend/build.gradle.kts backend/build.gradle.kts

RUN chmod +x gradlew && ./gradlew :backend:dependencies --no-daemon --quiet || true

COPY backend/src backend/src

RUN ./gradlew :backend:shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/backend/build/libs/backend-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
