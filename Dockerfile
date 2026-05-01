# ── Stage 1 : Build ───────────────────────────────────────────────────────────
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q \
    && mvn dependency:copy-dependencies -DoutputDirectory=target/deps -q

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q

# ── Stage 2 : distroless ──────────────────────────────────────────────────────
FROM gcr.io/distroless/java21-debian12:nonroot

WORKDIR /app

COPY --from=builder /app/target/deps  ./deps
COPY --from=builder /app/target/*.jar ./app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-cp", "app.jar:deps/*", \
  "me.noynto.eosa.Bootstrap"]