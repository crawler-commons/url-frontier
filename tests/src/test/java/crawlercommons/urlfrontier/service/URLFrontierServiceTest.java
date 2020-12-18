package crawlercommons.urlfrontier.service;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLItem.Status;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class URLFrontierServiceTest {

	private ManagedChannel channel;

	private URLFrontierStub frontier;

	private URLFrontierBlockingStub blockingFrontier;

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(URLFrontierServiceTest.class);

	@Before
	public void init() throws IOException {

		String host = System.getProperty("urlfrontier.host");
		String port = System.getProperty("urlfrontier.port");

		LOG.info("Initialisation of connection to URLFrontier service on {}:{}", host, port);

		channel = ManagedChannelBuilder.forAddress(host, Integer.parseInt(port)).usePlaintext().build();
		frontier = URLFrontierGrpc.newStub(channel);
		blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
	}

	@After
	public void shutdown() {
		LOG.info("Shutting down connection to URLFrontier service");
		channel.shutdown();
	}

	@Test
	public void testUpdates() {

		final AtomicBoolean completed = new AtomicBoolean(false);
		final AtomicInteger acked = new AtomicInteger(0);

		StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver = new StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>() {

			@Override
			public void onNext(crawlercommons.urlfrontier.Urlfrontier.String value) {
				// receives confirmation that the value has been received
				acked.addAndGet(1);
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

		URLItem item = URLItem.newBuilder().setKey("key1.com").setStatus(Status.DISCOVERED).setUrl("http://key1.com/")
				.setNextFetchDate(i.getEpochSecond()).build();

		// send a duplicate
		i = Instant.now();
		URLItem item2 = URLItem.newBuilder().setKey("key1.com").setStatus(Status.DISCOVERED).setUrl("http://key1.com/")
				.setNextFetchDate(i.getEpochSecond()).build();

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

		Assert.assertEquals("incorrect number of url acked", 2, acked.get());

		/** The methods below use the blocking API **/

		// check that we have one queue for it

		LOG.info("Checking existence of queue");

		GetParams request = GetParams.newBuilder().build();
		StringList queueslisted = blockingFrontier.listQueues(request);

		Assert.assertEquals("incorrect number of queues returned", 1, queueslisted.getStringList().size());

		LOG.info("Received {} queue - 1 expected", queueslisted.getStringList().size());

		/** Get the URLs due for fetching for a specific key **/

		// want just one URL for that specific key
		request = GetParams.newBuilder().setKey("key1.com").setMaxUrlsPerQueue(1).build();

		String urlreturned = blockingFrontier.getURLs(request).next().getUrl();

		Assert.assertEquals("incorrect number of URLs returned", "http://key1.com/", urlreturned);

		/** Get stats anout the queue **/

		Stats stats = blockingFrontier.stats(Urlfrontier.String.newBuilder().setValue("key1.com").build());

		Assert.assertEquals("incorrect number of queues from stats", 1, stats.getNumberOfQueues());

		// should still have one URL marked as in process
		Assert.assertEquals("incorrect number of inprocesss from stats", 1, stats.getInProcess());
	}

}
