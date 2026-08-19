# URL Frontier API

This module contains the [gRPC](https://grpc.io) schema used in a URL Frontier implementation as well as the Java code generated from it.

An automatically generated overview of the endpoints and messages can be found [here](urlfrontier.md)

# Main concepts

The endpoints and messages described by the API are meant to be a minimum. Specific implementations can offer additional functions on top of the ones described here.

The two main methods used by a crawler to interact with a URL Frontier service (let's just call it _service_ from now on) are:
- GetURLs
- PutURLs

with PutDiscovered as a batched alternative to PutURLs for the newly discovered URLs.

Underpinning them is the concept of *queue(s)* and *crawl(s)*.

## Queues and keys

What the queues should be based on is determined by the client, through the setting of a string value (_key_) associated with the messages sent with the PutURLs method. The value could be the hostname of a URL, its domain, IP or anything else. An empty value leaves the service to route the messages into a queue - the hostname being the default behaviour. It is up to the client code to be consistent in the use of the keys.

The keys are used in several functions: _GetStats_, _DeleteQueue_, _GetURLs_, _GetURLStatus_, _ListURLs_, _CountURLs_, _PurgeURLs_, _SetDelay_, _BlockQueueUntil_ and _SetCrawlLimit_.

## Crawls and crawlIDs

A service can handle one or more crawls, identified by a _crawlID_. Queues with the same keys can exist in multiple crawls and will be treated as totally distinct instances, i.e. deleting a queue for a particular crawl will not affect the queues with the same keys in other crawls. Similarly, a URL is considered unique to a crawl.

A crawl can be thought of as a namespace. The default _crawlID_ is 'DEFAULT' but it is expected that service implementations will handle empty strings as an equivalent (note: this is due to Protobuf not allowing to have default values for String fields).

## GetURLs

The service returns URLs ready to be fetched. It helps enforcing politeness by limiting the number of URLs per queue to be returned as well the amount of time to wait for until the URLs returned will be eligible to be returned again. This is used to prevent URLs to be in limbo if the client code crashes and is resumed later. It is easier to think about the URLs that have been returned by the GetURLs function as being _in transit_. They remain so until an update is received for them via the *PutURLs* function (see below) or the value set in *delay_requestable* has elapsed.

Internally, the service will rotate on the queues to be pulled from and will aim at an optimal distribution. Multiple clients can call _getURLs_ on a single instance and will each get URLs from across queues as URLs are prioritized by the service.

## PutURLs

This method is called to add newly discovered URLs to the frontier (e.g. they have been found while parsing a HTML document or a sitemap file) but also to update the information about URLs that had been previously obtained from *GetURLs* and have then been processed by the crawler. The latter allows to remove them from the _in transit_ status and so, more URLs can then be returned for its queue. Arbitrary metadata can be associated with a URL, for instance to store the depth of links followed since injecting the seeds or the HTTP code last obtained when a known URL has been fetched.

## PutDiscovered

Sends batches of discovered URLs rather than one message per URL. A URL is created unless it is already
known, in which case it is ignored - the same rule as a DiscoveredURLItem sent through *PutURLs*, and the
outlinks of a page form a natural batch. What limits the ingestion rate of a service is the per-message
cost, so grouping the discovered URLs - the bulk of what a crawl writes - is what a client should do to
push them faster; *PutURLs* remains the method to use for the updates of URLs which have been fetched.

Each batch is acknowledged as a whole, with one status per URL in the order they were sent.

## Discovered vs known

Discovered URLs are treated differently from known ones which are being updated. Discovered URLs will be added to the queues only if they are not already known, whereas known URLs will always be updated.

Another difference is in the scheduling of the URLs: discovered URLs are added to the queues (if they are unknown so far) without specific information about when they should be fetched - the service will return them as soon as possible. Known URLs on the other hand can have a _refetchable_from_date_ meaning that the service will put them back in the queues and serve them through _getURLs_ when the delay has elapsed. This is useful for instance when a transient error has occurred when fetching a URL, we might want to try it later. If no value is specified, the URL will be considered done and won't be returned by getURLs ever again.

## URL priority
The URLs are sorted by _refetchable_from_date_, which is typically the number seconds of UTC time since Unix epoch. The frontier checks that this value is lower or equal to the current timestamp in order to emit them. 
With that in mind, you can set any value you want as long as it is not 0 to prioritise URLs within a queue.

## Distributed mode
Some of the messages used by the API have a field _local_. This is used to indicate whether the action is pertaining only to the target node or to the whole cluster. For instance, the method _GetStats_ can return either the stats for the particular 
instance of the Frontier or the whole cluster.

# Out of scope

## URLFiltering
The filtering logic has to be handled within the crawlers as it is often application specific.

## Robots.txt
The robots directives are not stored within the URL Frontier.

--------------------------------------

# Maven dependencies

The Java code generated from the schema is available as a Maven dependency.

```
	<dependencies>
		<dependency>
			<groupId>com.github.crawler-commons</groupId>
			<artifactId>urlfrontier-API</artifactId>
			<version>2.5</version>
		</dependency>
	</dependencies>
```


# Code generation


The GRPC Java code is now generated automatically with the Protobuf Maven Plugin ( https://github.com/ascopes/protobuf-maven-plugin ).
There is no need anymore to download the protoc and protoc-gen-grpc compilers.
The protocol definition file (urlfrontier.proto) has been moved under src/main/protobuf.

Important note: The version of protoc downloaded from the Maven repository will not run natively on Alpine Linux
unless you install the gcompat package  which provides glibc compatibility (apk add gcompat).


For other languages, you need to generate the code stubs yourself, as shown here for Python. The commands
below are run from this directory, where the schema is _src/main/protobuf/urlfrontier.proto_.

```
python3 -m pip install grpcio-tools
mkdir python
python3 -m grpc_tools.protoc -I src/main/protobuf --python_out=python --grpc_python_out=python src/main/protobuf/urlfrontier.proto
```

Alternatively, [docker-protoc](https://github.com/namely/docker-protoc) can be used to generate the code in various languages:

```
docker run -v `pwd`/src/main/protobuf:/defs namely/protoc-all -f urlfrontier.proto -l go -o gen
```

# Documentation generation

[urlfrontier.md](urlfrontier.md) is generated from the schema with
[protoc-gen-doc](https://github.com/pseudomuto/protoc-gen-doc) and is committed alongside it. Regenerate it
whenever the schema changes, from the root of the project:

```
mvn -Pprotodoc generate-sources -pl API
```

The _protodoc_ profile is not part of the default build, so a normal `mvn package` neither needs
protoc-gen-doc nor touches the generated markdown.

