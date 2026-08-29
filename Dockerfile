# Multi stage so the runtime image carries no Maven, no source and no build cache.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies resolve in their own layer, so a source change does not re-download the world.
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
RUN mvn -B -q dependency:go-offline

COPY src/ src/
COPY config/ config/
# The quality gates already ran in CI; re-running them here would only slow the image build.
RUN mvn -B -q clean package -DskipTests -Dspotless.check.skip=true \
    -Dcheckstyle.skip=true -Dspotbugs.skip=true -Djacoco.skip=true

# Jammy rather than Alpine: the Alpine variant has no arm64 manifest, so the image would build in
# CI and fail on an Apple Silicon machine.
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# A non root user, because a container that does not need root should not have it.
RUN groupadd --system rex && useradd --system --gid rex --home /app rex
COPY --from=build --chown=rex:rex /build/target/*.jar app.jar
USER rex

EXPOSE 8080

# Container aware heap sizing, so the JVM respects the memory limit it was given rather than
# reading the host's total.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseSerialGC"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
