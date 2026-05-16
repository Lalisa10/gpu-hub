# syntax=docker/dockerfile:1.7

# Build context for this Dockerfile is the project root (NOT backend/),
# so the SPA can be built and baked into the JAR alongside the backend.

# ---- Stage 1: build the SPA ----
FROM node:22-alpine AS frontend
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ---- Stage 2: build the backend (with SPA baked into static/) ----
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /workspace

# Cache dependencies
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Build
COPY backend/src/ src/
COPY --from=frontend /app/dist/ src/main/resources/static/
RUN ./mvnw -B -q clean package -DskipTests \
    && mkdir -p /workspace/app \
    && cp target/*.jar /workspace/app/app.jar

# ---- Stage 3: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=backend /workspace/app/app.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
