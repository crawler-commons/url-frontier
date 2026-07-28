// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Reproduces issue #170: a gRPC handler thread must not be held while the work it handed over to an
 * executor is running. The response is closed by whichever thread finishes last instead.
 */
class NonBlockingStreamsTest {

    private static final String CRAWL_ID = "crawl_id";
    private static final String KEY = "queue_mysite";

    /** MemoryFrontierService whose write and read paths can be held on a latch. */
    static class BlockingService extends MemoryFrontierService {

        final CountDownLatch enteredPut = new CountDownLatch(1);
        final CountDownLatch releasePut = new CountDownLatch(1);
        final CountDownLatch enteredSend = new CountDownLatch(1);
        final CountDownLatch releaseSend = new CountDownLatch(1);

        volatile boolean blockPuts = false;
        volatile boolean blockSends = false;

        BlockingService() {
            super("localhost", 0);
        }

        @Override
        protected AckMessage.Status putURLItem(URLItem value) {
            if (blockPuts) {
                enteredPut.countDown();
                await(releasePut);
            }
            return super.putURLItem(value);
        }

        @Override
        protected int sendURLsForQueue(
                QueueInterface queue,
                QueueWithinCrawl key,
                int maxURLsPerQueue,
                int secsUntilRequestable,
                long now,
                SynchronizedStreamObserver<URLInfo> observer) {
            if (blockSends) {
                enteredSend.countDown();
                await(releaseSend);
            }
            return super.sendURLsForQueue(
                    queue, key, maxURLsPerQueue, secsUntilRequestable, now, observer);
        }

        private static void await(CountDownLatch latch) {
            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Counts the acks and signals when the response stream is closed. */
    static class AckCollector implements StreamObserver<AckMessage> {
        final AtomicInteger acked = new AtomicInteger();
        final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public void onNext(AckMessage value) {
            acked.incrementAndGet();
        }

        @Override
        public void onError(Throwable t) {
            closed.countDown();
        }

        @Override
        public void onCompleted() {
            closed.countDown();
        }
    }

    static class URLCollector implements StreamObserver<URLInfo> {
        final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public void onNext(URLInfo value) {}

        @Override
        public void onError(Throwable t) {
            closed.countDown();
        }

        @Override
        public void onCompleted() {
            closed.countDown();
        }
    }

    private static URLItem discovered(String url) {
        URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(CRAWL_ID).setKey(KEY).build();
        return URLItem.newBuilder()
                .setID(CRAWL_ID + "_" + url)
                .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                .build();
    }

    @Test
    void putURLsDoesNotBlockTheInboundStream() throws Exception {
        BlockingService service = new BlockingService();
        service.blockPuts = true;

        AckCollector acks = new AckCollector();
        StreamObserver<URLItem> put = service.putURLs(acks);
        put.onNext(discovered("https://www.mysite.com/a"));
        assertTrue(service.enteredPut.await(5, TimeUnit.SECONDS), "the write never started");

        // the handler thread must come straight back: it used to spin until the write
        // executor had drained
        assertTimeoutPreemptively(Duration.ofSeconds(5), put::onCompleted);
        assertEquals(
                1, acks.closed.getCount(), "the response must stay open while an item is unacked");

        service.releasePut.countDown();
        assertTrue(acks.closed.await(5, TimeUnit.SECONDS), "the response was never closed");
        assertEquals(1, acks.acked.get(), "the item must still be acked");

        service.close();
    }

    @Test
    void getURLsDoesNotBlockTheHandlerThread() throws Exception {
        BlockingService service = new BlockingService();

        AckCollector seeded = new AckCollector();
        StreamObserver<URLItem> put = service.putURLs(seeded);
        put.onNext(discovered("https://www.mysite.com/a"));
        put.onCompleted();
        assertTrue(seeded.closed.await(5, TimeUnit.SECONDS), "seeding did not complete");

        service.blockSends = true;

        // the rotation path, i.e. no key in the request: the queues are served by the
        // read executor
        GetParams request =
                GetParams.newBuilder().setMaxUrlsPerQueue(1).setDelayRequestable(30).build();
        URLCollector urls = new URLCollector();
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> service.getURLs(request, urls));

        assertTrue(service.enteredSend.await(5, TimeUnit.SECONDS), "the send never started");
        assertEquals(
                1, urls.closed.getCount(), "the response must stay open while a queue is served");

        service.releaseSend.countDown();
        assertTrue(urls.closed.await(5, TimeUnit.SECONDS), "the response was never closed");

        service.close();
    }
}
