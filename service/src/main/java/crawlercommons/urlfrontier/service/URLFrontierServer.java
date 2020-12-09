package crawlercommons.urlfrontier.service;

import java.io.IOException;

import org.slf4j.LoggerFactory;

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
		this.server = ServerBuilder.forPort(port).addService(new URLFrontierService()).build();
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
