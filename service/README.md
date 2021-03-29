# URL Frontier Service

Implementation of the URL Frontier Service.

The easiest way to run the Frontier is to use Docker and do 

```
 docker pull crawlercommons/url-frontier:0.1
 docker run --rm crawlercommons/url-frontier:0.1
```

The service will run on the default port (7071). Clients can connect to it using the gRPC code generated from the API.

## Compilation

To build and run the service from source, compile with `mvn clean package`

then run with 

`java -Xmx2G -cp target/urlfrontier-service-*.jar crawlercommons.urlfrontier.service.URLFrontierServer`


