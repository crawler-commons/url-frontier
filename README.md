# url-frontier

Discovering content on the web is possible thanks to web crawlers, luckily there are many excellent open source solutions for this; however, most of them have their own way of storing and accessing the information about the URLs. The aim of the *URL Frontier* project is to develop a crawler-neutral API for the operations that a web crawler when communicating with a web frontier e.g. get the next URLs to crawl, update the information about  URLs already processed, change the crawl rate for a particular hostname, get the list of active hosts, get statistics, etc... 

The idea being that it could be used by a variety of open source web crawlers, starting with [StormCrawler](http://stormcrawler.net) but also with Heritrix or Apache Nutch. The outcomes of the project are to design a REST API with OpenAPI then provide a set of client APIs using Swagger Codegen as well as a robust reference implementation and a validation suite to check that implementations behave as expected. 

One of the objectives of URL Frontier is to involve as many actors in the web crawling community as possible and get real users to give continuous feedback on our proposals. 

