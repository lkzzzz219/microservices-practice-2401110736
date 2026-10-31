FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml settings.xml ./
COPY src ./src
RUN mvn -s settings.xml -B -DskipTests package
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/order-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
