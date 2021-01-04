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

import java.io.IOException;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase;
import io.grpc.Server;
import io.grpc.ServerBuilder;

public class URLFrontierServer {

	public static final int DEFAULT_PORT = 7071;

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(URLFrontierServer.class);

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO READ CONFIG FILE AND TAKE PORT FROM THERE OR COMMAND LINE
		URLFrontierServer server = new URLFrontierServer(DEFAULT_PORT);
		server.start();
		server.blockUntilShutdown();
	}

	private Server server;

	public URLFrontierServer(int port) {
		// TODO make the implementation configurable
		URLFrontierImplBase service = new DummyURLFrontierService();
		this.server = ServerBuilder.forPort(port).addService(service).build();
	}

	public void start() throws IOException {
		server.start();
		LOG.info("Started URLFrontierServer on port {}", server.getPort());
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
