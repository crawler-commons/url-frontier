FROM maven:3-jdk-8 AS build

RUN useradd -m urlfrontier

USER urlfrontier

WORKDIR /home/urlfrontier

COPY --chown=urlfrontier pom.xml .

COPY --chown=urlfrontier API API
COPY --chown=urlfrontier client client
COPY --chown=urlfrontier service service
COPY --chown=urlfrontier tests tests

RUN mvn clean package -DskipFormatCode=true

RUN rm service/target/original-*.jar
RUN cp service/target/*.jar urlfrontier-service.jar

FROM openjdk:8-jdk-slim

RUN useradd -m urlfrontier

WORKDIR /home/urlfrontier

COPY --chown=urlfrontier --from=build /home/urlfrontier/urlfrontier-service.jar urlfrontier-service.jar

USER urlfrontier

ENTRYPOINT ["java", "-Xmx2G", "-jar", "urlfrontier-service.jar"]
