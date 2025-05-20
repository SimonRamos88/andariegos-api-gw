# ----- Build stage -----
FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

COPY src ./src

RUN ./mvnw clean package -DskipTests

# ----- Runtime stage -----
FROM eclipse-temurin:21-jre
# Usa variables de entorno opcionales
# ENV JAVA_OPTS=""
# ENV SPRING_PROFILES_ACTIVE=prod
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 7080

ENTRYPOINT ["java", "-jar", "app.jar"]
