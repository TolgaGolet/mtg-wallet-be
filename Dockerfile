FROM eclipse-temurin:21.0.13_9-jdk-jammy AS build

WORKDIR /build

# Copy the application code (includes the Maven wrapper in mvnw + .mvn/)
COPY . .

# Build with the project's Maven wrapper, so the image uses the same Maven version as local builds
RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

# Use the Eclipse Temurin 21 JRE image as the base for the final image
FROM eclipse-temurin:21.0.13_9-jre-jammy

WORKDIR /app

# Run as an unprivileged user instead of root
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin app

# Copy the built JAR file from the build image
COPY --from=build --chown=app:app /build/target/mtg-wallet-be-0.0.1-SNAPSHOT.jar mtg-wallet-be.jar

USER app

# Expose the port
EXPOSE 8080

# Container-level health check against the public actuator endpoint (curl ships with the Temurin image)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8080/api/actuator/health || exit 1

# Set the entry point for the application
ENTRYPOINT ["java","-jar","mtg-wallet-be.jar"]
