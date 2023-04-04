FROM maven:3.8.6-jdk-11 AS build
COPY . .
RUN mvn clean package -Pprod -DskipTests

FROM openjdk:11-jdk-slim
COPY --from=build /target/mtg-wallet-be-0.0.1-SNAPSHOT.jar mtg-wallet-be.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","mtg-wallet-be.jar"]