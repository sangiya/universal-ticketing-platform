# Runtime image for the TicketMesh backend.
# Uses the pre-built Spring Boot fat jar (built locally) to avoid re-resolving
# dependencies inside the container. See docker-compose.yml for env wiring.
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --create-home --shell /usr/sbin/nologin app
WORKDIR /app
# Build context is the repo root, so the artifact always comes from target/.
COPY target/ticketmesh-core-*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=10 --start-period=30s \
  CMD wget -q -O - http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app/app.jar"]
