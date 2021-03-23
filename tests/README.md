# URL Frontier Test Suite

Runs a series of tests against a running instance of a URLFrontier service.

The service is expected to run locally on port 7071, but the hostname and port values can be overridden on the command line e.g.

`mvn -Durlfrontier.host=127.0.0.1 -Durlfrontier.port=7071 -Dmaven.test.skip=false test`

The test suite also serves as an example of how to communicate with the service using gRPC.
