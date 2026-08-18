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
  ListNodes    Prints out list of nodes forming the cluster
  ListQueues   Prints out active queues
  ListCrawls   Prints out list of crawls
  ListURLs     Prints out all URLs in the Frontier
  GetStats     Prints out stats from the Frontier
  PutURLs      Send URLs from a file into a Frontier
  GetURLs      Get URLs from a Frontier and display in the standard output
  SetActive    Pause or resume the Frontier
  GetActive    Check whether the Frontier has been paused
  DeleteQueue  Delete a queue from the Frontier
  DeleteCrawl  Delete an entire crawl from the Frontier
  SetLogLevel  Change the log level of a package in the Frontier service
  CountURLs    Counts the number of URLs in a Frontier
  DumpURLs     Export all the URLs of a Frontier as JSON, one per line, in the
                 format taken by PutURLs; used to migrate the data to a
                 different backend or version.
```

## Exporting and re-importing the data

`DumpURLs` writes every URL of a Frontier, with its queue key, crawl ID, metadata and scheduling
information, as one JSON object per line - the exact format `PutURLs` takes as input. This makes it
possible to migrate the data to a different backend implementation or a different major version:

```
java -jar ./target/urlfrontier-client*.jar DumpURLs -o frontier.jsonl.gz
java -jar ./target/urlfrontier-client*.jar -t newhost PutURLs -f frontier.jsonl.gz
```

By default the content is streamed one crawl at a time; with `-t` the client lists the queues
first and several threads dump them in parallel, which spreads the work over the server's cores:

```
java -jar ./target/urlfrontier-client*.jar DumpURLs -t 8 -o frontier.jsonl.gz
```

The dump covers only the node the client connects to; in a cluster, dump every node and
concatenate the files before re-importing. For a complete and consistent dump, deactivate the
Frontier (`SetActive -s false`) and stop injecting URLs while it runs. Note that the creation date
of the URLs is not preserved by a dump / re-import cycle: the target Frontier assigns it at import
time, which affects `PurgeURLs`.
