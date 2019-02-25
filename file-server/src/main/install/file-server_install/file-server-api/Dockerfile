FROM openjdk:8-jdk

RUN mkdir -p /usr/share/file-server/bin/config

COPY file-server-2.2.0-SNAPSHOT.jar /usr/share/file-server/bin

EXPOSE 8266

WORKDIR /usr/share/file-server/bin

CMD java -jar /usr/share/file-server/bin/file-server-2.2.0-SNAPSHOT.jar