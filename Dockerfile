# Stage 1 — build (Maven ships in image; no wrapper CRLF issues on Windows hosts)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -q
COPY src/ src/
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/intelligent-content-management-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
