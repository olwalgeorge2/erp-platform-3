# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . /workspace
RUN ./gradlew :api-gateway:quarkusBuild -x test -Dquarkus.package.type=fast-jar --no-daemon

FROM eclipse-temurin:21-jre
ENV QUARKUS_HTTP_PORT=8080
WORKDIR /app
COPY --from=build /workspace/api-gateway/build/quarkus-app/ ./
EXPOSE 8080
ENTRYPOINT ["java","-jar","quarkus-run.jar"]

