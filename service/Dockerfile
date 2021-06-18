FROM maven:3-jdk-8 AS build

RUN mkdir /urlfrontier

# copy the source tree and the pom.xml to our new container
COPY ./src /urlfrontier/src
COPY ./pom.xml /urlfrontier

WORKDIR /urlfrontier

RUN mvn clean package

RUN rm target/original-*.jar
RUN cp target/*.jar urlfrontier-service.jar

FROM openjdk:8-jdk-slim

COPY --from=build /urlfrontier/urlfrontier-service.jar urlfrontier-service.jar

ENTRYPOINT ["java", "-Xmx2G", "-jar", "urlfrontier-service.jar"]
