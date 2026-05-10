FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY gateway/build/libs/gateway-*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
