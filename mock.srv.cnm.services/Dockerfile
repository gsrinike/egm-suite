FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -DskipTests \
    -Ddocker.skip=true \
    -Ddocker.skip.build=true \
    -Ddocker.skip.push=true \
    -pl mock.srv.cnm.services \
    -am \
    package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/mock.srv.cnm.services/target/mock.srv.cnm.services-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
