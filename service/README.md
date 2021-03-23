# URL Frontier Service

Simple server implementing the API.

Compile with `mvn clean package`

then run with 

`java -Xmx2G -cp target/urlfrontier-service-*.jar crawlercommons.urlfrontier.service.URLFrontierServer`

the service will run on port _7071_. Clients can connect to it using the gRPC code generated from the API.
