FROM openjdk:21-jdk-slim AS build

# Copy the application code
COPY . .

# Build the application with Maven
RUN mvn clean package -DskipTests

# Use the openjdk 21 image as the base for the final image
FROM openjdk:21-jdk-slim

# Copy the built JAR file from the build image
COPY --from=build /target/mtg-wallet-be-0.0.1-SNAPSHOT.jar mtg-wallet-be.jar

# Expose the port
EXPOSE 8080

# Set the entry point for the application
ENTRYPOINT ["java","-jar","mtg-wallet-be.jar"]