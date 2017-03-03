# styx-explorer
A web-based UI for managing STXY data (and a showcase for styx-data and styx-http)

## Running

    java -cp target/styx-explorer.jar:"target/lib/*" styx.explorer.Explorer
    java -cp target/styx-explorer.jar:"target/lib/*" styx.data.db.DatabaseAdmin

## Docker

    docker build -t styx-explorer .
    docker run -d --name myexplorer -p 8080:8080 -v ~/styx-data:/usr/local/styx-explorer/data styx-explorer
