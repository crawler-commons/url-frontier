# Logging and Monitoring

URL Frontier provides logging and metrics that can be integrated with
common monitoring systems.

## Logging

The reference service uses Logback for logging.

By default, logs are written to the console at `INFO` level and above.

A custom Logback configuration can be supplied when starting the Java
service by setting the `logback.configurationFile` system property.

The service also exposes a `SetLogLevel` operation, and the command-line
client can change the logging level for a package while the service is
running.

## Prometheus metrics

The service can expose metrics for Prometheus.

The metrics port is configured with the service `-s` option.

For example, deployments commonly expose the gRPC service separately from
the Prometheus metrics endpoint.

## Grafana

The repository includes a Grafana dashboard definition for visualizing
metrics exported by URL Frontier.

[Grafana dashboard](https://github.com/crawler-commons/url-frontier/blob/master/service/monitoring/provisioning/dashboards/URLFrontier-Prometheus.json)

## Loki

Older URL Frontier documentation included examples for forwarding logs to
Grafana Loki.

Those examples depended on specific Loki logging libraries and Java
versions and are not included here because they can become outdated.

URL Frontier console or Logback output can instead be integrated with the
logging infrastructure used by the deployment environment.

## More information

See the service documentation for the current logging and metrics options:

[Service README](https://github.com/crawler-commons/url-frontier/blob/master/service/README.md)
