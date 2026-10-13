FROM gradle:8.14-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENV TZ=America/Lima
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
