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

# JVM options, override with e.g. docker run -e JAVA_OPTS="-Xms4G -Xmx4G -XX:+UseG1GC"
ENV JAVA_OPTS="-Xms2G -Xmx2G -XX:+UseG1GC"

# shell form so that JAVA_OPTS is expanded; exec keeps java as PID 1 so that it gets the signals
# from docker stop and "--" fills $0 so that the arguments of docker run are passed on as "$@"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar urlfrontier-service.jar \"$@\"", "--"]
