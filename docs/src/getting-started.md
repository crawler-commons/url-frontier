# Getting Started

URL Frontier provides a common API and reference service for managing URLs
during web crawling.

The easiest way to try URL Frontier is to run the reference service with
Docker.

## Run with Docker

Pull the latest image:

    docker pull crawlercommons/url-frontier

Start the service:

    docker run --rm --name frontier -p 7071:7071 -p 9100:9100 crawlercommons/url-frontier -s 9100

The URL Frontier service listens on port `7071` by default.

## Persist frontier data

The default service implementation uses RocksDB. To persist its data outside
the container, mount a local directory:

    docker run --rm --name frontier -v /pathOnDisk:/crawldir -p 7071:7071 crawlercommons/url-frontier rocksdb.path=/crawldir/rocksdb

## Memory

The JVM in the container runs with a 2 GB heap (`-Xms2G -Xmx2G -XX:+UseG1GC`).
To change it, set the `JAVA_OPTS` environment variable, which replaces these
options entirely:

    docker run --rm --name frontier -p 7071:7071 -e JAVA_OPTS="-Xms4G -Xmx4G -XX:+UseG1GC" crawlercommons/url-frontier

RocksDB allocates its memory outside the Java heap, so if you limit the
container with `--memory`, keep the heap well below that limit.

## Build from source

Clone the repository:

    git clone https://github.com/crawler-commons/url-frontier.git
    cd url-frontier

Build the project with Maven:

    mvn clean package
