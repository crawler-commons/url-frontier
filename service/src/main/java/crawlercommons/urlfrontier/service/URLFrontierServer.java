/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons SPDX-License-Identifier: Apache-2.0 Licensed to
 * Crawler-Commons under one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership. DigitalPebble licenses
 * this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.service.rocksdb.RocksDBService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "URL Frontier Server", mixinStandardHelpOptions = true, version = "2.0-SNAPSHOT")
public class URLFrontierServer implements Callable<Integer> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(URLFrontierServer.class);

    @Option(
            names = {"-p", "--port"},
            defaultValue = "7071",
            paramLabel = "NUM",
            description = "URL Frontier port (default to 7071)")
    int port;

    @Option(
            names = {"-h", "--hostname"},
            required = false,
            defaultValue = "localhost",
            paramLabel = "STRING",
            description = "hostname value to report - defaults to localhost")
    String host;

    @Option(
            names = {"-c", "--config"},
            required = false,
            paramLabel = "STRING",
            description = "key value configuration file")
    String config;

    @Option(
            names = {"-s", "--prometheus_server"},
            paramLabel = "NUM",
            description = "Port number to use for Prometheus server")
    int prometheus_server;

    @Parameters List<String> positional;

    private Server server;

    private AbstractFrontierService service = null;

    private HTTPServer prometheus = null;

    public static void main(String... args) {
        CommandLine cli = new CommandLine(new URLFrontierServer());
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        try {
            start();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        return 0;
    }

    public void start() throws Exception {

        // default implementation
        String implementationClassName = RocksDBService.class.getName();

        Map<String, String> configuration = new HashMap<>();

        // Do we want to expose Metrics via a Prometheus server?
        if (prometheus_server > 0) {
            /**
             * Register Prometheus collectors for garbage collection, memory pools, classloading,
             * and thread counts.
             */
            DefaultExports.initialize();

            LOG.info("Starting Prometheus server on port {}", prometheus_server);
            prometheus = new HTTPServer.Builder().withPort(prometheus_server).build();
        }

        if (config != null) {
            try {
                // check that the file exists
                if (!new File(config).exists()) {
                    LOG.error("Config file not found : {}", config);
                    System.exit(-1);
                }
                // populate the config with the content of the file
                for (String line : Files.readAllLines(Paths.get(config))) {
                    line = line.trim();
                    if (line.startsWith("#")) continue;
                    addToConfig(configuration, line);
                }
            } catch (Exception e) {
                LOG.error("Exception caught when reading the configuration from {}", config, e);
                System.exit(-1);
            }
        }

        // override or add K/V with entries from args
        if (positional != null) {
            for (String l : positional) {
                addToConfig(configuration, l);
            }
        }

        // get the implementation class from the config if set
        implementationClassName =
                configuration.getOrDefault("implementation", implementationClassName);

        Class<?> implementationClass = Class.forName(implementationClassName);

        if (!AbstractFrontierService.class.isAssignableFrom(implementationClass)) {
            LOG.error(
                    "Implementation class {} does not extend AbstractFrontierService",
                    implementationClassName);
            System.exit(-1);
        }

        // can it take a Map as constructor?
        if (configuration.size() > 0) {
            try {
                Constructor<?> c = implementationClass.getConstructor(Map.class);
                c.setAccessible(true);
                service = (AbstractFrontierService) c.newInstance(configuration);
            } catch (NoSuchMethodException e) {
                LOG.info(
                        "Implementation {} does not have a constructor taking a Map as argument",
                        implementationClassName);
            } catch (Exception e) {
                LOG.error("Exception caught when initialising the service", e);
                System.exit(-1);
            }
        }

        if (service == null) {
            try {
                service = (AbstractFrontierService) implementationClass.newInstance();
            } catch (Exception e) {
                LOG.error("Exception caught when initialising the service", e);
                System.exit(-1);
            }
        }

        service.setHostAndPort(host, port);

        this.server = ServerBuilder.forPort(port).addService(service).build();
        server.start();
        LOG.info(
                "Started URLFrontierServer [{}] on port {}",
                service.getClass().getSimpleName(),
                server.getPort());

        registerShutdownHook();

        blockUntilShutdown();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    try {
                                        stop();
                                    } catch (Exception e) {
                                        LOG.error(
                                                "Error when trying to shutdown a lifecycle component: "
                                                        + this.getClass().getName(),
                                                e);
                                    }
                                }));
    }

    public void stop() {
        // terminate the service if possible
        if (service != null && Closeable.class.isAssignableFrom(service.getClass())) {
            LOG.info("Shutting down service {}", service.getClass().getSimpleName());
            try {
                ((Closeable) service).close();
            } catch (IOException e) {
                LOG.error("Error when closing service: ", e);
            }
        }

        if (server != null) {
            LOG.info("Shutting down URLFrontierServer on port {}", server.getPort());
            server.shutdown();
        }

        if (prometheus != null) {
            LOG.info("Shutting down Prometheus server on port {}", prometheus.getPort());
            prometheus.close();
        }
    }

    /** Await termination on the main thread since the grpc library uses daemon threads. */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static final void addToConfig(final Map<String, String> config, String line) {
        if (line == null || line.length() == 0) return;

        line = line.trim();

        if (line.length() == 0) return;

        // = to separate key from value
        int pos = line.indexOf('=');
        // no value
        if (pos == -1) {
            config.put(line, null);
            return;
        }
        String key = line.substring(0, pos).trim();
        String value = line.substring(pos + 1).trim();
        config.put(key, value);
    }
}
