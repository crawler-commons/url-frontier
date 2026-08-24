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

## Build from source

Clone the repository:

    git clone https://github.com/crawler-commons/url-frontier.git
    cd url-frontier

Build the project with Maven:

    mvn clean package
