FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY user-service/build/libs/user-service-*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
