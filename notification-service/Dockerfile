FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY notification-service/build/libs/notification-service-*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
