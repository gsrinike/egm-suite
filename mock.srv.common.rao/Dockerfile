FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -DskipTests -Ddocker.skip=true -Ddocker.skip.build=true -Ddocker.skip.push=true -pl mock.srv.common.rao -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/mock.srv.common.rao/target/mock.srv.common.rao-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
