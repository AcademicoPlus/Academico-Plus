FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
