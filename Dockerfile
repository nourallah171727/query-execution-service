# ================================
# 1. Builder stage
# ================================
FROM maven:3.9.9-eclipse-temurin-17-focal AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
COPY query-exec-frontend ./query-exec-frontend
COPY docs ./docs

RUN mvn -B clean package -DskipTests


# ================================
# 2. Runtime stage
# ================================
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/query-execution-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]