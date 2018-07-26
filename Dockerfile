FROM openjdk:8-jre-alpine
MAINTAINER K. Alex Mills <k.alex.mills@gmail.com>

EXPOSE 46747

ADD target/lib           /usr/share/gossipcar/lib
ARG JAR_FILE

ADD target/${JAR_FILE}   /usr/share/gossipcar/gossip-car.jar

WORKDIR /usr/share/gossipcar
ENTRYPOINT [ "/usr/bin/java", "-jar", "./user-service.jar" ]