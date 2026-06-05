# Build multi-stage do gateway. O context deve ser a RAIZ do repo,
# pois o build do Gradle gera código a partir de ../proto.
#   docker build -f p/Dockerfile -t gateway .
# (o docker-compose já faz isso com context: .)

# --- build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY p_rest ./p_rest
WORKDIR /workspace/p_rest
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

# --- runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copia só o boot jar (o *-plain.jar não casa com *-SNAPSHOT.jar).
COPY --from=build /workspace/p_rest/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
