/**
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

import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "URL Frontier Server", mixinStandardHelpOptions = true, version = "1.0")
public class URLFrontierServer implements Callable<Integer> {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(URLFrontierServer.class);

	@Option(names = { "-p",
			"--port" }, defaultValue = "7071", paramLabel = "NUM", description = "URL Frontier port (default to 7071)")
	int port;

	private Server server;

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
		// TODO make the implementation configurable
		URLFrontierImplBase service = new DummyURLFrontierService();

		this.server = ServerBuilder.forPort(port).addService(service).build();
		server.start();
		LOG.info("Started URLFrontierServer on port {}", server.getPort());

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
