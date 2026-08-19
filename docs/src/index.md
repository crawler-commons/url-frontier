# URL Frontier

URL Frontier provides a crawler- and language-neutral API for the operations
web crawlers perform when interacting with a crawl frontier.

Different web crawlers have traditionally used their own approaches for
storing and accessing information about URLs. URL Frontier provides a common
API that crawlers can use regardless of their implementation language.

## What does URL Frontier provide?

The project provides:

- a gRPC API for interacting with a URL frontier;
- a reference URL Frontier service implementation;
- a command-line client for interacting with the service;
- a test suite for validating URL Frontier implementations.

## Main operations

URL Frontier supports common crawler frontier operations such as:

- retrieving URLs that are ready to crawl;
- adding newly discovered URLs;
- updating URLs that have already been processed;
- controlling crawl rates;
- managing queues and crawls;
- retrieving frontier statistics.

## Project components

The project is divided into several main components:

- **API** - the gRPC schema and generated Java API.
- **Service** - the reference implementation of the URL Frontier service.
- **Client** - a command-line client for interacting with a frontier.
- **Tests** - a test suite for verifying URL Frontier implementations.
