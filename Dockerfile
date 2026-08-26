FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src/ ./src
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/team-skeleton.jar app.jar
RUN addgroup -S appgroup
RUN adduser -S appuser -g appgroup
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]