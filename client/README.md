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
  ListQueues  Prints out active queues
  GetStats    Prints out stats from the Frontier
  PutURLs     Send URLs from a file into a Frontier
```
