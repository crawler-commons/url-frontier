# API

URL Frontier defines a gRPC API that web crawlers can use to communicate
with a frontier service.

The API module contains:

- the Protocol Buffers schema;
- generated Java code for the API;
- generated documentation for endpoints and messages.

## Main operations

The main crawler-facing operations are:

- `GetURLs` - retrieve URLs that are ready to be fetched;
- `PutURLs` - add or update individual URLs;
- `PutDiscovered` - submit newly discovered URLs in batches.

Other operations are available for managing queues, crawls, statistics,
delays, crawl limits, and URL status.

## API reference

The complete generated API reference is available in the repository:

[Generated API reference](https://github.com/crawler-commons/url-frontier/blob/master/API/urlfrontier.md)

The Protocol Buffers schema is located under:

    API/src/main/protobuf/urlfrontier.proto

## Language support

The project provides generated Java code directly.

Clients using other programming languages can generate their own gRPC
stubs from the Protocol Buffers schema.
