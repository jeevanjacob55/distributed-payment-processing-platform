FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY ledger-service/pom.xml ledger-service/pom.xml
COPY fraud-service/pom.xml fraud-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY ledger-service/src ledger-service/src
RUN mvn -pl ledger-service -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ledger-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
