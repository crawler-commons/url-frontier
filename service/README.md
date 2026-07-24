# URL Frontier Service

[![Docker Image Version (latest semver)](https://img.shields.io/docker/v/crawlercommons/url-frontier)](https://hub.docker.com/r/crawlercommons/url-frontier)
[![Docker Pulls](https://img.shields.io/docker/pulls/crawlercommons/url-frontier)](https://hub.docker.com/r/crawlercommons/url-frontier)

Implementations of the URL Frontier Service. There are currently 2 implementations available:
- a simple memory-based which was used primarily for testing
- the default one which is scalable, persistent and is based on [RocksDB](https://rocksdb.org/)

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

For implementation supporting a cluster mode, it is required to use the parameter `-h xxx.xxx.xxx.xxx` with the private IP or hostname
on which it is running so that it can report its location with the heartbeat.

## Distributed mode

The sharded implementation (`crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService`) runs the Frontier as a cluster of nodes, each owning a partition of the queues.

### Configuration and queue ownership

Every node is started with the `nodes` parameter, a comma-separated list of `host:port` addresses. All nodes must be configured with the same unique set of addresses — the order does not matter, the list is sorted internally — and each node must appear in it exactly once, be started with `-h`/port matching its own entry, and use its own local RocksDB directory.

A queue (key + crawl ID) is owned by exactly one node, determined by hashing the queue identifier over the sorted node list. URLs sent to any node are forwarded to the owner. Changing the set of nodes remaps queue ownership without migrating the data, so treat cluster resizes as a rebuild.

### Control operations

Since 2.6, the control RPCs act on the whole cluster no matter which node receives them. The `local` field on each request restricts the call to the receiving node instead. Forwarded calls carry a per-RPC deadline.

| RPC | `local=false` (default) | `local=true` |
|---|---|---|
| `SetDelay` with a key | routed to the queue owner | this node only |
| `SetDelay` without a key | default delay broadcast to every node | this node only |
| `BlockQueueUntil` | routed to the queue owner (empty key: local no-op, as historically) | this node only |
| `SetCrawlLimit` | routed to the queue owner (the crawl ID is normalized; empty key fails) | this node only |
| `SetActive` | applied locally, then broadcast to every node | this node only |
| `GetActive` | logical AND across all nodes: a mixed cluster reports `false`, an unreachable node is an error rather than `false`; the result is not an atomic snapshot | this node only |

Broadcasts are not atomic: when an error is returned, some nodes may already have applied the change. Repeating the same call is idempotent in the absence of concurrent newer writes.

### Reservation and politeness guarantees

`GetURLs` takes all its per-queue eligibility decisions (blocked state, crawl limit, politeness delay, in-process cap) atomically. After a send returns one or more URLs, the queue cannot be served again inside its delay window, regardless of how many clients poll concurrently. A send that returns zero URLs restores the previous state, so an empty queue is not penalized and can be retried immediately.

Once a queue-control RPC completes, reservations started afterwards observe the updated state; a send that was already reserved when the RPC arrived may still go out.

### Persistence and restart behaviour

RocksDB persists URL records and reconstructs queue counts, but control and lease state is held in memory. A restarted node loses:

- per-queue delays and the default delay;
- `blockedUntil` timestamps;
- crawl limits;
- the active state;
- the politeness window (last-served timestamps);
- the in-process leases.

After a restart, clients must re-assert the delays, blocks and limits they rely on, and URLs that were in flight may be served again.

### Deployment and security assumptions

Inter-node channels are plaintext and the RPCs are unauthenticated: the service must only be exposed on a private, trusted network. Coordinated upgrades of all nodes are recommended — in a mixed-version cluster, calls forwarded to an older node behave as that version did.

See #148, #150, #151, #156 and #157 for the details behind these semantics.

## Logging configuration

The logging is done with Logback. A default configuration is loaded and will dump logs on the console at INFO level and above but the configuration
file can be overriden with

`java -Xmx2G -Dlogback.configurationFile=test.xml ...`

Alternatively, the Frontier service has a _SetLogLevel_ endpoint and the CLI allows to set the level for a given package from the console.

## Metrics with Prometheus

The service implementation takes a parameter *-s*, the value of which is used as port number to expose metrics for [Prometheus](https://prometheus.io/).
A [dashboard](https://github.com/crawler-commons/url-frontier/blob/2.x/service/monitoring/provisioning/dashboards/URLFrontier-Prometheus.json) for Grafana is provided.

## Docker

The easiest way to run the Frontier is to use Docker

```
 docker pull crawlercommons/url-frontier
 docker run --rm --name frontier -p 7071:7071 -p 9100:9100  crawlercommons/url-frontier -s 9100
```

The service will run on the default port (7071). Additional parameters can simply be added to the command, for instance, to persist RocksDB between runs

```
docker run --rm --name frontier -v /pathOnDisk:/crawldir -p 7071:7071 crawlercommons/url-frontier rocksdb.path=/crawldir/rocksdb
```

Specify a config file with a volume and the `-c` flag:

```
docker run --rm --name frontier -p 7071:7071 -p 9100:9100 -v /path/to/config.ini:/config/config.ini ufrontier -s 9100 -c /config/config.ini
```