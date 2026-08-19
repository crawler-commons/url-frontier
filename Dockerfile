# The shaded jar is platform-independent (no native classifiers; rocksdbjni and
# grpc-netty-shaded ship the natives for every arch), so the build runs once on
# the builder's own platform instead of once per target under emulation.
FROM --platform=$BUILDPLATFORM maven:3-eclipse-temurin-17 AS build

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

FROM eclipse-temurin:17-jdk-noble

RUN useradd -m urlfrontier

WORKDIR /home/urlfrontier

COPY --chown=urlfrontier --from=build /home/urlfrontier/urlfrontier-service.jar urlfrontier-service.jar

USER urlfrontier

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-XX:+UseG1GC", "-jar", "urlfrontier-service.jar"]
