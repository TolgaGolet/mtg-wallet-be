FROM eclipse-temurin:21-jdk-jammy AS build

# Install Maven
RUN apt-get update \
    && apt-get install -y maven \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy the application code
COPY . .

# Build the application with Maven
RUN mvn clean package -DskipTests

# Use the Eclipse Temurin 21 JRE image as the base for the final image
FROM eclipse-temurin:21-jre-jammy

# Copy the built JAR file from the build image
COPY --from=build /target/mtg-wallet-be-0.0.1-SNAPSHOT.jar mtg-wallet-be.jar

# Expose the port
EXPOSE 8080

# Set the entry point for the application
ENTRYPOINT ["java","-jar","mtg-wallet-be.jar"]
