$ mvn clean verify
$ java -cp target/data-explorer.jar:"target/lib/*" com.github.phoswald.data.explorer.DataExplorer
$ java -cp target/data-explorer.jar:"target/lib/*" styx.data.db.DatabaseAdmin
$ docker build -t data-explorer .
$ docker run -d --name mycontainer -p 8080:8080 -v ~/styx-data:/usr/local/data-explorer/data data-explorer
