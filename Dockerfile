# ----- Build stage -----
FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

# ----- Runtime stage -----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el jar compilado
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto HTTPS
EXPOSE 7080

ENTRYPOINT ["java","-jar","app.jar"]

