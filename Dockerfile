# ---- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Cache dependencies separately from sources so code changes rebuild fast.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 appuser
COPY --from=build /workspace/target/product-inventory-api-*.jar app.jar
USER appuser

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD ["sh", "-c", "wget -qO- http://localhost:8080/actuator/health | grep -q UP"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
