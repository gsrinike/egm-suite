FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE=target/srv.cgm.importer-0.1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
