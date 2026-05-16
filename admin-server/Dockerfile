FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"] 