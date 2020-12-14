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
import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.Timestamp;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLItem.Status;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class URLFrontierServiceTest {

	private ManagedChannel channel;

	private URLFrontierStub frontier;

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(URLFrontierServiceTest.class);

	@Before
	public void init() throws IOException {

		String host = System.getProperty("urlfrontier.host");
		String port = System.getProperty("urlfrontier.port");

		LOG.info("Initialisation of connection to URLFrontier service on {}:{}", host, port);

		channel = ManagedChannelBuilder.forAddress(host, Integer.parseInt(port)).usePlaintext().build();
		frontier = URLFrontierGrpc.newStub(channel);
	}

	@After
	public void shutdown() {
		LOG.info("Shutting down connection to URLFrontier service");
		channel.shutdown();
	}

	@Test
	public void testUpdates() {

		final AtomicBoolean completed = new AtomicBoolean(false);

		StreamObserver<Empty> responseObserver = new StreamObserver<Empty>() {

			@Override
			public void onNext(Empty value) {
			}

			@Override
			public void onError(Throwable t) {
				completed.set(true);
				LOG.info("Error received", t);
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

		// send a duplicate
		i = Instant.now();
		URLItem item2 = URLItem.newBuilder().setKey("key1.com").setStatus(Status.DISCOVERED).setUrl("http://key1.com/")
				.setNextFetchDate(ts).build();

		streamObserver.onNext(item);
		streamObserver.onNext(item2);

		streamObserver.onCompleted();

		LOG.info("Sending URL: {}", item);

		// wait for completion
		while (completed.get() == false) {
			try {
				Thread.currentThread().sleep(10);
			} catch (InterruptedException e) {
			}
		}

		completed.set(false);

		AtomicInteger numQueues = new AtomicInteger(0);

		// check that we have one queue for it

		LOG.info("Checking existence of queue");

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
				LOG.info("Error received", t);
			}

			@Override
			public void onCompleted() {
				completed.set(true);
			}
		};

		GetParams request = GetParams.newBuilder().build();
		frontier.listQueues(request, responseObserver2);

		// wait for completion
		while (completed.get() == false) {
			try {
				Thread.currentThread().sleep(10);
			} catch (InterruptedException e) {
			}
		}

		Assert.assertEquals("incorrect number of queues returned", 1, numQueues.intValue());

		LOG.info("Received {} queue - 1 expected", numQueues.intValue());

		/** Get the URLs due for fetching for a specific key **/

		completed.set(false);

		String[] urlreturned = new String[1];

		// want just one URL for that specific key
		request = GetParams.newBuilder().setKey("key1.com").setMaxUrlsPerQueue(1).build();

		StreamObserver<URLItem> responseObserver3 = new StreamObserver<Urlfrontier.URLItem>() {

			@Override
			public void onNext(URLItem value) {
				urlreturned[0] = value.getUrl();
			}

			@Override
			public void onError(Throwable t) {
				completed.set(true);
				LOG.info("Error received", t);
			}

			@Override
			public void onCompleted() {
				completed.set(true);
			}
		};

		frontier.getURLs(request, responseObserver3);

		// wait for completion
		while (completed.get() == false) {
			try {
				Thread.currentThread().sleep(10);
			} catch (InterruptedException e) {
			}
		}
		
		Assert.assertEquals("incorrect number of URLs returned", "http://key1.com/", urlreturned[0]);

	}

}
