# syntax=docker/dockerfile:1.7

# ---- Stage 1: build with Maven ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Cache dependencies
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Build
COPY src/ src/
RUN ./mvnw -B -q clean package -DskipTests \
    && mkdir -p /workspace/app \
    && cp target/*.jar /workspace/app/app.jar

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app/app.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
