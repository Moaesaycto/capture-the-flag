FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app
COPY gradlew .
COPY gradle gradle/
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew
COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY ./src/main/resources/certs/ctf-local.p12 /app/certs/ctf-local.p12
RUN mkdir -p /app/config
EXPOSE 8080 8443

ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar", "--spring.config.location=file:/app/config/"]