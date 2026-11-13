# Build the complete Maven reactor because backend depends on the algorithm engine.
FROM maven:3.9.16-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY algorithm-engine/pom.xml algorithm-engine/pom.xml
COPY backend/pom.xml backend/pom.xml
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline -pl backend -am

COPY algorithm-engine algorithm-engine
COPY backend backend
COPY database database
RUN mvn --batch-mode --no-transfer-progress package -DskipTests -pl backend -am

FROM eclipse-temurin:21.0.13_9-jre

RUN groupadd --system app && useradd --system --gid app --create-home app
WORKDIR /app
COPY --from=build /workspace/backend/target/backend-*.jar /app/app.jar
RUN chown app:app /app/app.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
