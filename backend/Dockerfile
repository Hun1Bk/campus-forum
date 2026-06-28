FROM maven:3.8.8-eclipse-temurin-8 AS build

WORKDIR /app
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x ./mvnw
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:8-jre

WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads

EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
