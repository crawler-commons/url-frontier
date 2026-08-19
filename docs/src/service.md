# Service

URL Frontier includes a reference service implementation that exposes the
frontier API over gRPC.

The service currently provides:

- a simple in-memory implementation intended mainly for testing;
- a persistent RocksDB-based implementation used by default.

## Running the service

The service listens on port `7071` by default.

Configuration can be provided through a configuration file or with
command-line parameters.

The default RocksDB data directory is:

    ./rocksdb

## Configuration

Common configuration options include:

- `implementation` - selects the frontier service implementation;
- `rocksdb.path` - sets the RocksDB storage directory;
- `server.enable_reflection` - enables gRPC reflection;
- `read.thread.num` - controls threads serving `GetURLs`;
- `write.thread.num` - controls threads applying URL updates.

## Distributed mode

URL Frontier also supports a sharded RocksDB implementation for running
the frontier across multiple nodes.

Queues are assigned to nodes based on their queue key and crawl ID.

## Metrics

The service can expose Prometheus metrics on a configured port.

A Grafana dashboard is also provided in the repository for monitoring
URL Frontier metrics.

## More information

See the complete service documentation in the repository:

[Service README](https://github.com/crawler-commons/url-frontier/blob/master/service/README.md)
