FROM maven:3-jdk-8 AS build

RUN useradd -m urlfrontier

USER urlfrontier

WORKDIR /home/urlfrontier

# copy the source tree and the pom.xml to our new container
COPY --chown=urlfrontier ./src src
COPY --chown=urlfrontier ./pom.xml .

RUN mvn clean package

RUN rm target/original-*.jar
RUN cp target/*.jar urlfrontier-service.jar

FROM openjdk:8-jdk-slim

RUN useradd -m urlfrontier

WORKDIR /home/urlfrontier

COPY --chown=urlfrontier --from=build /home/urlfrontier/urlfrontier-service.jar urlfrontier-service.jar

USER urlfrontier

ENTRYPOINT ["java", "-Xmx2G", "-jar", "urlfrontier-service.jar"]
