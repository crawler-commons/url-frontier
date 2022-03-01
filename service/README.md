# URL Frontier Service

[![Docker Image Version (latest semver)](https://img.shields.io/docker/v/crawlercommons/url-frontier)](https://hub.docker.com/r/crawlercommons/url-frontier)
[![Docker Pulls](https://img.shields.io/docker/pulls/crawlercommons/url-frontier)](https://hub.docker.com/r/crawlercommons/url-frontier)

Implementation of the URL Frontier Service. There are currently 2 implementations available:
- a simple memory-based which was used primarily for testing
- the default one which is scalable, persistent and is based on [RocksDB](https://rocksdb.org/).

Web crawlers can connect to it using the gRPC code generated from the API. There is also a simple client available 
which can do basic interactions with a Frontier.

## Compilation

To build and run the service from source, compile with `mvn clean package`

## Execution

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

If no path is set explicitly for RocksDB,  the default value _./rocksdb_ will be used.

## Logging configuration

The logging is done with Logback. A default configuration is loaded and will dump logs on the console at INFO level and above but the configuration 
file can be overriden with 

`java -Xmx2G -Dlogback.configurationFile=test.xml ...`

Alternatively, the Frontier service has a _SetLogLevel_ endpoint and the CLI allows to set the level for a given package from the console.

## Metrics with Prometheus 

The service implementation takes a parameter *-s*, the value of which is used as port number to expose metrics for [Prometheus](https://prometheus.io/).

## Docker

The easiest way to run the Frontier is to use Docker

```
 docker pull crawlercommons/url-frontier
 docker run --rm --name frontier -p 7071:7071 crawlercommons/url-frontier
```

The service will run on the default port (7071). Additional parameters can simply be added to the command, for instance, to persist RocksDB between runs

`docker run --rm --name frontier -v /pathOnDisk:/crawldir -p 7071:7071 crawlercommons/url-frontier rocksdb.path=/crawldir/rocksdb`
