/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons
 * SPDX-License-Identifier: Apache-2.0
 * Licensed to Crawler-Commons under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package crawlercommons.urlfrontier.service;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "URL Frontier Server", mixinStandardHelpOptions = true, version = "0.2")
public class URLFrontierServer implements Callable<Integer> {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(URLFrontierServer.class);

	@Option(names = { "-p",
			"--port" }, defaultValue = "7071", paramLabel = "NUM", description = "URL Frontier port (default to 7071)")
	int port;

	@Option(names = { "-c",
			"--config" }, required = false, paramLabel = "STRING", description = "JSON configuration file")
	String config;

	private Server server;

	private URLFrontierImplBase service = null;

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

		String implementationClassName = MemoryFrontierService.class.getName();

		JsonNode configurationNode = null;

		if (config != null) {
			try {
				configurationNode = new ObjectMapper().readTree(new File(config));
				implementationClassName = configurationNode.get("implementation").asText();
			} catch (Exception e) {
				LOG.error("Exception caught when reading the configuration from {}", config, e);
				System.exit(-1);
			}
		}

		Class<?> implementationClass = Class.forName(implementationClassName);

		if (!URLFrontierImplBase.class.isAssignableFrom(implementationClass)) {
			LOG.error("Implementation class {} does not extend URLFrontierImplBase", implementationClassName);
			System.exit(-1);
		}

		// can it take a JSON node as constructor?
		if (configurationNode != null) {
			try {
				Constructor<?> c = implementationClass.getConstructor(JsonNode.class);
				c.setAccessible(true);
				service = (URLFrontierImplBase) c.newInstance(configurationNode);
			} catch (Exception e) {
				LOG.error("Exception caught when initialising the service", e);
				System.exit(-1);
			}
		}

		if (service == null) {
			service = (URLFrontierImplBase) implementationClass.newInstance();
		}

		this.server = ServerBuilder.forPort(port).addService(service).build();
		server.start();
		LOG.info("Started URLFrontierServer [{}] on port {}", service.getClass().getSimpleName(), server.getPort());

		registerShutdownHook();

		blockUntilShutdown();
	}

	private void registerShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				stop();
			} catch (Exception e) {
				LOG.error("Error when trying to shutdown a lifecycle component: " + this.getClass().getName(), e);
			}
		}));
	}

	public void stop() {
		// terminate the service if possible
		if (service != null && Closeable.class.isAssignableFrom(service.getClass())) {
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
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

}
