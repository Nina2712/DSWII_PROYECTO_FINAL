# ---------- Etapa 1: compilar ----------
FROM gradle:8.14-jdk21 AS build
WORKDIR /app
# Primero las dependencias (se reutiliza la capa si no cambia el build)
COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon -q > /dev/null 2>&1 || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home medicita
COPY --from=build /app/build/libs/app.jar app.jar
USER medicita

ENV TZ=America/Lima \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Duser.timezone=America/Lima"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
