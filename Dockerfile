FROM maven:3.8.6-jdk-11 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:11-jdk-slim
VOLUME /tmp
COPY --from=build /target/mtg-wallet-be-0.0.1-SNAPSHOT.jar mtg-wallet-be.jar
ENTRYPOINT ["java","-jar","/mtg-wallet-be.jar"]
EXPOSE 8080
