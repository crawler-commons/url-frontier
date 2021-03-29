FROM maven:3.6.3-jdk-8

RUN apt install sed

RUN mkdir /urlfrontier

# copy the source tree and the pom.xml to our new container
COPY ./src /urlfrontier/src
COPY ./pom.xml /urlfrontier

WORKDIR /urlfrontier

# package our application code
RUN mvn clean package

RUN rm target/original-*.jar

RUN cp target/*.jar urlfrontier-service.jar

RUN rm -rf target
RUN rm -rf src
RUN rm pom.xml
 
ENTRYPOINT ["java", "-Xmx2G", "-jar", "urlfrontier-service.jar"]
