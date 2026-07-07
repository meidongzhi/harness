FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY demo-app/pom.xml .
COPY demo-app/src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/companion-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
