# URLFrontier monitoring

This directory contains basic resources for [Grafana](https://grafana.com/) and [Loki](https://grafana.com/oss/loki/) for monitoring a [URLFrontier](http://urlfrontier.net) setup.
[Prometheus](https://prometheus.io/) is used to store the metrics displayed by Grafana.

[URLFrontier 1.2](https://github.com/crawler-commons/url-frontier/releases/tag/urlfrontier-1.2) is needed in order to send metrics to Prometheus. 

## Prerequisites

You need to have [Docker](https://docs.docker.com/get-docker/) and [docker-compose](https://docs.docker.com/compose/install/#install-compose) installed. 

The next step is to install the Loki driver so that the logs in the Frontier containers can be sent to Loki.

`docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions`

## Instructions

Start the Docker containers with `docker-compose up -d`.

Open Grafana on [http://localhost:3000/login](http://localhost:3000/login), enter _admin_ , _admin_  then skip the creation of a new password. In the section _Recently viewed dashboards_, click on _URL Frontier_.

You can then start interaction with URLFrontier, e.g. using the [client](https://github.com/crawler-commons/url-frontier/releases/download/urlfrontier-1.1/urlfrontier-client-1.1.jar):

`java -jar ~/urlfrontier-client-1.1.jar PutURLs -f seeds.txt` where seeds is a file containing URLs.

After a while you should see the metrics for the incoming and outgoing URLs as well as the logs from the Frontier at the bottom of the dashboard.


