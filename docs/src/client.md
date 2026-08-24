# Command-line Client

URL Frontier includes a Java command-line client for basic interactions
with a running frontier service.

## Connecting

By default, the client connects to:

- host: `localhost`;
- port: `7071`.

The host and port can be changed with command-line options.

## Available operations

The client provides commands for tasks such as:

- listing cluster nodes, queues, and crawls;
- listing URLs stored in the frontier;
- retrieving frontier statistics;
- injecting discovered URLs;
- retrieving URLs ready to crawl;
- pausing and resuming the frontier;
- deleting queues or crawls;
- setting crawl limits;
- checking URL status;
- counting and purging URLs;
- exporting frontier data.

Every client command also provides its own `--help` output.

## URL injection

The `PutURLs` command can read URLs from a file.

It can also use multiple threads, batch discovered URLs with `PutDiscovered`,
and limit the number of URLs waiting for acknowledgement.

## Exporting data

The `DumpURLs` command exports frontier data as JSON Lines.

The exported data can later be passed back to `PutURLs`, which makes the
client useful for migrating frontier data between backends or versions.

## More information

See the complete client documentation in the repository:

[Command-line client README](https://github.com/crawler-commons/url-frontier/blob/master/client/README.md)
