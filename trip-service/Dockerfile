FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY trip-service/build/libs/trip-service-*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
