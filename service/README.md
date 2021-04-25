# URL Frontier Service

Implementation of the URL Frontier Service.

The easiest way to run the Frontier is to use Docker and do 

```
 docker pull crawlercommons/url-frontier:0.1
 docker run --rm --name frontier -p 7071:7071 crawlercommons/url-frontier:0.1
```

The service will run on the default port (7071). Clients can connect to it using the gRPC code generated from the API.

## Compilation

To build and run the service from source, compile with `mvn clean package`

then run with 

`java -Xmx2G -cp target/urlfrontier-service-*.jar crawlercommons.urlfrontier.service.URLFrontierServer`

You can specify the implementation to use for the service and its configuration by passing a configuration file with '-c'.

The configuration file below will set RocksDBService as the implementation to use and configure the path where its data should be stored. 

```
implementation = crawlercommons.urlfrontier.service.rocksdb.RocksDBService
rocksdb.path = /pathToCrawlDir/rocksdb
```

The key values from the configuration file can also be passed on the command line. Since the RocksDBService is the default implementation, 
the call above can have the following equivalent without the config file:

`java -Xmx2G -cp target/urlfrontier-service-*.jar crawlercommons.urlfrontier.service.URLFrontierServer rocksdb.path=/pathToCrawlDir/rocksdb` 

