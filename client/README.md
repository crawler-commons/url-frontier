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
```
