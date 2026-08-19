# Core Concepts

URL Frontier uses a small set of concepts that allow different web crawlers
to interact with the same frontier API.

## Queues and keys

URLs are organized into queues.

The client can associate a string value called a **key** with URLs sent to
the frontier. The key determines which queue the URLs belong to.

A key can represent values such as:

- a hostname;
- a domain;
- an IP address;
- another grouping selected by the crawler.

If the key is empty, the service can determine the queue. The hostname is
the default behavior.

## Crawls and crawl IDs

A URL Frontier service can handle one or more independent crawls.

Each crawl is identified by a **crawl ID** (`crawlID`). Queues with the same
key can exist in different crawls without affecting each other.

A crawl can therefore be considered a namespace.

The default crawl ID is `DEFAULT`.

## GetURLs

`GetURLs` returns URLs that are ready to be fetched.

The service helps enforce crawler politeness by controlling when URLs from
a queue can be returned.

URLs returned by `GetURLs` are considered in transit until they are updated
by the crawler or their requestable delay expires.

## PutURLs

`PutURLs` is used to add URLs and to update URLs that have already been
processed by the crawler.

Metadata can also be associated with a URL, such as crawl depth, HTTP status
information, or scheduling information.

## PutDiscovered

`PutDiscovered` provides a batched way to submit newly discovered URLs.

Batching reduces the per-message cost and improves ingestion throughput when
large numbers of URLs are discovered.

## Discovered and known URLs

Discovered URLs are added only when they are not already known to the
frontier.

Known URLs can be updated and can use `refetchable_from_date` to schedule
another fetch in the future.
