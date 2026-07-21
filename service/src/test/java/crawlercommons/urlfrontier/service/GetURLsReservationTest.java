// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Reproduces issue #147: the politeness gate in getURLs must be atomic — a queue must not be served
 * more than once inside its delay window when requests are concurrent.
 */
class GetURLsReservationTest {

    static final String CRAWL_ID = "crawl_id";
    static final String KEY = "queue_mysite";

    /** MemoryFrontierService whose sendURLsForQueue can block on a latch, return 0, or throw. */
    static class InstrumentedMemoryService extends MemoryFrontierService {
        final AtomicInteger sendInvocations = new AtomicInteger();
        final CountDownLatch enteredSend = new CountDownLatch(1);
        final CountDownLatch releaseSend = new CountDownLatch(1);
        volatile boolean blockFirstSend = false;
        volatile boolean returnZero = false;
        volatile boolean throwOnFirstSend = false;

        InstrumentedMemoryService() {
            super("localhost", 0);
        }

        @Override
        protected int sendURLsForQueue(
                QueueInterface queue,
                QueueWithinCrawl key,
                int maxURLsPerQueue,
                int secsUntilRequestable,
                long now,
                SynchronizedStreamObserver<URLInfo> observer) {
            int invocation = sendInvocations.incrementAndGet();
            if (invocation == 1) {
                if (throwOnFirstSend) {
                    throw new RuntimeException("simulated send failure");
                }
                if (blockFirstSend) {
                    enteredSend.countDown();
                    try {
                        releaseSend.await(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (returnZero) {
                return 0;
            }
            return super.sendURLsForQueue(
                    queue, key, maxURLsPerQueue, secsUntilRequestable, now, observer);
        }
    }

    static class CollectingObserver implements StreamObserver<URLInfo> {
        final AtomicInteger received = new AtomicInteger();
        final CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void onNext(URLInfo value) {
            received.incrementAndGet();
        }

        @Override
        public void onError(Throwable t) {
            completed.countDown();
        }

        @Override
        public void onCompleted() {
            completed.countDown();
        }
    }

    /** Seeds two due URLs into KEY/CRAWL_ID and returns the queue with its delay set to 300s. */
    static QueueInterface seedQueue(InstrumentedMemoryService service) throws Exception {
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger acked = new AtomicInteger();
        StreamObserver<URLItem> put =
                service.putURLs(
                        new StreamObserver<>() {
                            @Override
                            public void onNext(AckMessage value) {
                                acked.incrementAndGet();
                            }

                            @Override
                            public void onError(Throwable t) {
                                done.set(true);
                            }

                            @Override
                            public void onCompleted() {
                                done.set(true);
                            }
                        });
        for (String url : new String[] {"https://www.mysite.com/a", "https://www.mysite.com/b"}) {
            URLInfo info =
                    URLInfo.newBuilder().setUrl(url).setCrawlID(CRAWL_ID).setKey(KEY).build();
            put.onNext(
                    URLItem.newBuilder()
                            .setID(CRAWL_ID + "_" + url)
                            .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                            .build());
        }
        put.onCompleted();
        while (!done.get() || acked.get() != 2) {
            Thread.sleep(5);
        }
        QueueInterface queue = service.getQueues().get(QueueWithinCrawl.get(KEY, CRAWL_ID));
        queue.setDelay(300);
        return queue;
    }

    @Test
    void rotationPathServesQueueOnlyOncePerWindow() throws Exception {
        InstrumentedMemoryService service = new InstrumentedMemoryService();
        seedQueue(service);
        service.blockFirstSend = true;

        GetParams request =
                GetParams.newBuilder().setMaxUrlsPerQueue(1).setDelayRequestable(30).build();

        CollectingObserver first = new CollectingObserver();
        Thread t1 = new Thread(() -> service.getURLs(request, first));
        t1.start();
        assertTrue(service.enteredSend.await(5, TimeUnit.SECONDS), "first send never started");

        // while the first send is in flight, a concurrent request must skip the queue
        CollectingObserver second = new CollectingObserver();
        service.getURLs(request, second);
        assertTrue(second.completed.await(5, TimeUnit.SECONDS));

        service.releaseSend.countDown();
        t1.join(5000);

        assertEquals(
                1,
                service.sendInvocations.get(),
                "queue was served more than once inside its delay window");
    }

    @Test
    void keyedPathServesQueueOnlyOncePerWindow() throws Exception {
        InstrumentedMemoryService service = new InstrumentedMemoryService();
        seedQueue(service);
        service.blockFirstSend = true;

        GetParams request =
                GetParams.newBuilder()
                        .setKey(KEY)
                        .setCrawlID(CRAWL_ID)
                        .setMaxUrlsPerQueue(1)
                        .setDelayRequestable(30)
                        .build();

        CollectingObserver first = new CollectingObserver();
        Thread t1 = new Thread(() -> service.getURLs(request, first));
        t1.start();
        assertTrue(service.enteredSend.await(5, TimeUnit.SECONDS), "first send never started");

        CollectingObserver second = new CollectingObserver();
        service.getURLs(request, second);
        assertTrue(second.completed.await(5, TimeUnit.SECONDS));

        service.releaseSend.countDown();
        t1.join(5000);

        assertEquals(
                1,
                service.sendInvocations.get(),
                "queue was served more than once inside its delay window");
    }

    @Test
    void zeroSentDoesNotPenalizeQueue() throws Exception {
        InstrumentedMemoryService service = new InstrumentedMemoryService();
        QueueInterface queue = seedQueue(service);
        service.returnZero = true;

        GetParams request =
                GetParams.newBuilder().setMaxUrlsPerQueue(1).setDelayRequestable(30).build();

        CollectingObserver first = new CollectingObserver();
        service.getURLs(request, first);
        assertTrue(first.completed.await(5, TimeUnit.SECONDS));

        // nothing was sent: the previous lastProduced must be restored
        assertEquals(0, queue.getLastProduced());

        // and the queue is immediately reservable again
        CollectingObserver second = new CollectingObserver();
        service.getURLs(request, second);
        assertTrue(second.completed.await(5, TimeUnit.SECONDS));
        assertEquals(2, service.sendInvocations.get());
    }

    @Test
    void inFlightReservationBlocksBeyondDelayWindow() throws Exception {
        InstrumentedMemoryService service = new InstrumentedMemoryService();
        QueueInterface queue = seedQueue(service);
        queue.setDelay(5);
        long now = 100;

        // package-private helpers are declared on AbstractFrontierService
        AbstractFrontierService frontier = service;

        long previous = frontier.tryReserveQueue(queue, now, Integer.MAX_VALUE);
        assertNotEquals(AbstractFrontierService.RESERVE_FAILED, previous);

        // a timestamp-based reservation would have expired here; the in-flight marker holds
        assertEquals(
                AbstractFrontierService.RESERVE_FAILED,
                frontier.tryReserveQueue(queue, now + 5 + 1, Integer.MAX_VALUE));

        frontier.finalizeReservation(queue, now, previous, 3, true);
        assertEquals(now, queue.getLastProduced());

        // after finalization the normal delay rules apply again
        assertEquals(
                AbstractFrontierService.RESERVE_FAILED,
                frontier.tryReserveQueue(queue, now + 5, Integer.MAX_VALUE));
        assertNotEquals(
                AbstractFrontierService.RESERVE_FAILED,
                frontier.tryReserveQueue(queue, now + 6, Integer.MAX_VALUE));
    }

    @Test
    void exceptionDuringSendFailsClosedAndDoesNotHang() throws Exception {
        InstrumentedMemoryService service = new InstrumentedMemoryService();
        QueueInterface queue = seedQueue(service);
        service.throwOnFirstSend = true;

        GetParams request =
                GetParams.newBuilder().setMaxUrlsPerQueue(1).setDelayRequestable(30).build();

        // without the try/finally around sendURLsForQueue this call never returns
        CollectingObserver first = new CollectingObserver();
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> service.getURLs(request, first));

        // fail-closed: outcome unknown, so the politeness window is enforced
        assertNotEquals(AbstractFrontierService.RESERVATION_IN_PROGRESS, queue.getLastProduced());
        assertNotEquals(0, queue.getLastProduced());

        // a second request inside the window must not serve the queue
        CollectingObserver second = new CollectingObserver();
        service.getURLs(request, second);
        assertTrue(second.completed.await(5, TimeUnit.SECONDS));
        assertEquals(1, service.sendInvocations.get());
    }
}
