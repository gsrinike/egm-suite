FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -DskipTests \
    -Ddocker.skip=true \
    -Ddocker.skip.build=true \
    -Ddocker.skip.push=true \
    -pl srv.iidm.transformer \
    -am \
    package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/srv.iidm.transformer/target/srv.iidm.transformer-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8098
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
