package crawlercommons.urlfrontier.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import crawlercommons.urlfrontier.service.URLFrontierServer;

import crawlercommons.urlfrontier.*;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.Timestamp;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLItem.Status;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class URLFrontierServiceTester {

	URLFrontierServer server;

	private ManagedChannel channel;

	private URLFrontierStub frontier;

	@Before
	public void loadServer() throws IOException {
		int port = 6060;

		server = new URLFrontierServer(port);
		server.start();

		String host = "localhost";

		channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		frontier = URLFrontierGrpc.newStub(channel);
	}

	@After
	public void shutdown() {
		channel.shutdown();
		server.stop();
	}

	@Test
	public void testUpdates() {

		final AtomicBoolean completed = new AtomicBoolean(false);

		StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {

			@Override
			public void onNext(Empty value) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onError(Throwable t) {
				completed.set(true);
			}

			@Override
			public void onCompleted() {
				completed.set(true);
			}
		};

		StreamObserver<URLItem> streamObserver = frontier.putURLs(responseObserver);

		Instant i = Instant.now();
		Timestamp ts = Timestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();

		URLItem item = URLItem.newBuilder().setKey("key1.com").setStatus(Status.DISCOVERED).setUrl("http://key1.com/")
				.setNextFetchDate(ts).build();

		streamObserver.onNext(item);

		streamObserver.onCompleted();

		// wait for completion
		while (completed.get() == false) {
			try {
				Thread.currentThread().sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		completed.set(false);

		AtomicInteger numQueues = new AtomicInteger(0);

		// check that we have one queue for it
		GetParams request = GetParams.newBuilder().build();
		StreamObserver<StringList> responseObserver2 = new StreamObserver<Urlfrontier.StringList>() {

			@Override
			public void onNext(Urlfrontier.StringList value) {
				Iterator<String> iter = value.getStringList().iterator();
				while (iter.hasNext()) {
					iter.next();
					numQueues.incrementAndGet();
				}
			}

			@Override
			public void onError(Throwable t) {
				completed.set(true);
			}

			@Override
			public void onCompleted() {
				completed.set(true);
			}
		};

		frontier.listQueues(request, responseObserver2);

		// wait for completion
		while (completed.get() == false) {
			try {
				Thread.currentThread().sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		Assert.assertEquals("incorrect number of queues returned", 1, numQueues.intValue());

	}

}
