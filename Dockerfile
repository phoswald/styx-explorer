FROM openjdk:8-jre-alpine
COPY target/data-explorer.jar /usr/local/data-explorer/
COPY target/lib               /usr/local/data-explorer/lib
RUN mkdir /usr/local/data-explorer/data
EXPOSE 8080
WORKDIR /usr/local/data-explorer
CMD exec java -DdataStoreUrl=jdbc:h2:/usr/local/data-explorer/data/datastore -cp data-explorer.jar:"lib/*" com.github.phoswald.data.explorer.DataExplorer
