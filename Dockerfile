# -------- Stage 1: Build the application --------
FROM eclipse-temurin:17-jdk-alpine AS build

ENV APP_HOME=/app
WORKDIR $APP_HOME

# Install required tools (Maven, Git for .mvn wrapper)
RUN apk add --no-cache maven git

# Copy Maven wrapper and POM first to cache dependencies
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Pre-fetch dependencies (build layer caching)
RUN ./mvnw dependency:go-offline

# Copy the application source code
COPY src ./src

# Build the application, skip tests for faster CI builds
RUN ./mvnw clean package -DskipTests

# -------- Stage 2: Create a minimal runtime image --------
FROM gcr.io/distroless/java17-debian11:nonroot

# Use a non-root user for security
USER nonroot

# Set working directory
WORKDIR /app

# Explicitly copy only the final artifact
COPY --from=build /app/target/*.jar app.jar

# Set runtime command
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
