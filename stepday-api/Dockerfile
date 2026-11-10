FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY stepday-api/pom.xml stepday-api/
RUN mvn -B -q -pl stepday-api dependency:go-offline
COPY stepday-api/src stepday-api/src
RUN mvn -B -q -pl stepday-api package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /src/stepday-api/target/stepday-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
