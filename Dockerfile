FROM openjdk:8-jre-alpine
COPY target/styx-explorer.jar /usr/local/styx-explorer/
COPY target/lib               /usr/local/styx-explorer/lib
RUN mkdir /usr/local/styx-explorer/data
EXPOSE 8080
WORKDIR /usr/local/styx-explorer
CMD exec java -cp styx-explorer.jar:"lib/*" styx.explorer.Explorer
