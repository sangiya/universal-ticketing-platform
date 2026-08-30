# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:21-jre
RUN useradd --create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=build /workspace/target/ticketmesh-core-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app/app.jar"]
