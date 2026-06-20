FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/com.auth-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
