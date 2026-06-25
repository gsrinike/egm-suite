FROM eclipse-temurin:21-jre
ARG JAR_FILE=target/srv.cnm.services-0.1.0-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
