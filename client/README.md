# Command line client

Implemented in Java for simple interactions with a URLFrontier server

## Compilation

`mvn clean package`

## Execution

`java -jar ./target/urlfrontier-client*.jar`

```
Usage: Client [-hV] [-p=NUM] [-t=STRING] [COMMAND]
Interacts with a URL Frontier from the command line
  -h, --help          Show this help message and exit.
  -p, --port=NUM      URL Frontier port (default to 7071)
  -t, --host=STRING   URL Frontier hostname (defaults to 'localhost')
  -V, --version       Print version information and exit.
Commands:
  ListNodes      Prints out list of nodes forming the cluster
  ListQueues     Prints out active queues
  ListCrawls     Prints out list of crawls
  ListURLs       Prints out all URLs in the Frontier
  GetStats       Prints out stats from the Frontier
  PutURLs        Send URLs from a file into a Frontier
  GetURLs        Get URLs from a Frontier and display in the standard output
  SetActive      Pause or resume the Frontier
  GetActive      Check whether the Frontier has been paused
  DeleteQueue    Delete a queue from the Frontier
  DeleteCrawl    Delete an entire crawl from the Frontier
  SetLogLevel    Change the log level of a package in the Frontier service
  SetCrawlLimit  Set crawl limit for specific queue
  GetURLStatus   Get the status of an URL
  CountURLs      Counts the number of URLs in a Frontier
  PurgeURLs      Purge old URLs in a Frontier
  DumpURLs       Export all the URLs of a Frontier as JSON, one per line, in
                   the format taken by PutURLs; used to migrate the data to a
                   different backend or version.
```

Every command takes `--help`, which lists its own options.

## Injecting URLs

`PutURLs` reads one URL per line, or one JSON object per line in the format `DumpURLs` produces;
the file is decompressed on the fly if its name ends in `.gz`.

```
java -jar ./target/urlfrontier-client*.jar PutURLs -f seeds.txt
```

Three options matter for the throughput of a large injection:

- `-t/--threads` sends on several streams at once - the URLs are shared between the threads, none
  is sent twice;
- `-b/--batch` groups that many *discovered* URLs into one `PutDiscovered` message instead of
  sending them one at a time through `PutURLs`. What limits the ingestion rate is the per-message
  cost, so batching the discovered URLs - the bulk of what a crawl writes - is what makes the
  difference. Known URLs always go through `PutURLs`, and the client falls back to sending
  everything individually against a server which does not implement `PutDiscovered`;
- `-w/--in-flight` caps how many URLs a thread may have sent but not had confirmed (10000 by
  default). The service applies its own cap on top, see `putURLs.max.inflight` and
  `putDiscovered.max.inflight` in the [service configuration](../service/README.md).

```
java -jar ./target/urlfrontier-client*.jar PutURLs -f seeds.txt.gz -t 4 -b 100
```

## Exporting and re-importing the data

`DumpURLs` writes every URL of a Frontier, with its queue key, crawl ID, metadata and scheduling
information, as one JSON object per line - the exact format `PutURLs` takes as input. This makes it
possible to migrate the data to a different backend implementation or a different major version:

```
java -jar ./target/urlfrontier-client*.jar DumpURLs -o frontier.jsonl.gz
java -jar ./target/urlfrontier-client*.jar -t newhost PutURLs -f frontier.jsonl.gz
```

By default the content is streamed one crawl at a time; with `-t/--threads` (not to be confused
with the global `-t/--host`, which comes before the command name) the client lists the queues
first and several threads dump them in parallel, which spreads the work over the server's cores:

```
java -jar ./target/urlfrontier-client*.jar DumpURLs -t 8 -o frontier.jsonl.gz
```

The dump covers only the node the client connects to; in a cluster, dump every node and
concatenate the files before re-importing. For a complete and consistent dump, deactivate the
Frontier (`SetActive -s false`) and stop injecting URLs while it runs. Note that the creation date
of the URLs is not preserved by a dump / re-import cycle: the target Frontier assigns it at import
time, which affects `PurgeURLs`.
