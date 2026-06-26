# Backend requires Java 21 (see pom.xml java.version).
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN java -version && mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/IncidentPulse-0.0.1-SNAPSHOT.jar ./IncidentPulse.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "IncidentPulse.jar"]
