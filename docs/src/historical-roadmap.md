# Historical Roadmap

This page preserves a summary of the former URL Frontier 2 roadmap.

It is historical documentation and should not be treated as the current
project roadmap.

The roadmap described work funded through the NGI0 Discovery fund and
focused on turning URL Frontier into a more production-ready system.

## Main areas

The roadmap focused on three main areas:

- monitoring and reporting;
- discovery and clustering;
- robustness and resilience.

It also discussed multi-tenancy as an optional area of work.

## Monitoring and reporting

Planned work included configurable logging, metrics exposure, Prometheus,
Grafana dashboards, and investigation of Loki integration.

## Discovery and clustering

The roadmap proposed distributing frontier storage and processing across
multiple nodes and allowing operations to work across a cluster.

## Robustness and resilience

The roadmap also described improvements to crash recovery, restart time,
and possible replication between nodes.

## Multi-tenancy

The roadmap proposed introducing crawl IDs so that multiple logical crawls
could be handled independently.

Many of these concepts are now represented in the current URL Frontier
implementation and documentation.

For current behavior, use the Service, Core Concepts, and Logging and
Monitoring pages instead.
