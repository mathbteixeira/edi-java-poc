# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy the pom.xml and download dependencies (caches this layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application, skipping tests for speed
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port your Spring Boot API runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]