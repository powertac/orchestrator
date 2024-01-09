FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /opt/powertac/orchestrator/build
COPY . .
RUN mvn clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/powertac/orchestrator
ENV ORCHESTRATOR_JAR=orchestrator-0.3.0.jar
COPY --from=build /opt/powertac/orchestrator/build/target/${ORCHESTRATOR_JAR} ./${ORCHESTRATOR_JAR}
ENTRYPOINT java -jar /opt/powertac/orchestrator/${ORCHESTRATOR_JAR}