package crawlercommons.urlfrontier.service;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class URLFrontierServer {

	public static void main(String[] args) throws IOException, InterruptedException {
		URLFrontierServer server = new URLFrontierServer(6060);
		server.start();
		server.blockUntilShutdown();
	}

	private Server server;

	public URLFrontierServer(int port) throws IOException {
		this(ServerBuilder.forPort(port), port);
	}

	/**
	 * Create a RouteGuide server using serverBuilder as a base and features as
	 * data.
	 */
	public URLFrontierServer(ServerBuilder<?> serverBuilder, int port) {
		server = serverBuilder.addService(new URLFrontierService()).build();
	}

	public void start() throws IOException {
		server.start();
	}

	public void stop() {
		if (server != null) {
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
