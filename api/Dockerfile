# =============================================================
# SGTC - API (Spring Boot)
# Multi-stage: Maven 3.9 + JDK 21 -> Eclipse Temurin JRE 21
# =============================================================

# ----- Stage 1: build -----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copia somente o pom primeiro para aproveitar o cache de dependencias.
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

# Copia o codigo e empacota.
COPY src ./src
RUN mvn -B -DskipTests package \
    && cp target/*.jar app.jar

# ----- Stage 2: runtime -----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl: usado pelo healthcheck do docker-compose. Usuario nao-root para reduzir superficie.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system sgtc \
    && useradd --system --gid sgtc --shell /usr/sbin/nologin sgtc \
    && mkdir -p /var/sgtc/uploads \
    && chown -R sgtc:sgtc /var/sgtc

COPY --from=build /workspace/app.jar /app/app.jar

USER sgtc
EXPOSE 8080

# Permite override de JAVA_OPTS pelo docker-compose / k8s.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
