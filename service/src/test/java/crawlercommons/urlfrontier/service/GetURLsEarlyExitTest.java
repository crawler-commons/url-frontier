// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.service.GetURLsReservationTest.CollectingObserver;
import crawlercommons.urlfrontier.service.GetURLsReservationTest.InstrumentedMemoryService;
import crawlercommons.urlfrontier.service.memory.URLQueue;
import io.grpc.stub.ServerCallStreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * getURLs must not hand queues with nothing scheduled over to the backend, and must stop looking
 * for queues once the call is cancelled or all its tokens have been taken.
 */
class GetURLsEarlyExitTest {

    static final GetParams ROTATION_REQUEST =
            GetParams.newBuilder().setMaxUrlsPerQueue(1).setDelayRequestable(30).build();

    /** Counts the sends; fake queues have nothing to send, real ones go to the memory backend. */
    static class CountingService extends InstrumentedMemoryService {
        final AtomicInteger sends = new AtomicInteger();
        final CountDownLatch firstSendDone = new CountDownLatch(1);

        @Override
        protected int sendURLsForQueue(
                QueueInterface queue,
                QueueWithinCrawl key,
                int maxURLsPerQueue,
                int secsUntilRequestable,
                long now,
                SynchronizedStreamObserver<URLInfo> observer) {
            sends.incrementAndGet();
            try {
                if (queue instanceof URLQueue) {
                    return super.sendURLsForQueue(
                            queue, key, maxURLsPerQueue, secsUntilRequestable, now, observer);
                }
                return 0;
            } finally {
                firstSendDone.countDown();
            }
        }
    }

    /** A queue which always passes the politeness gate and runs a hook when it is looked at. */
    static class FakeQueue implements QueueInterface {
        final int active;
        final AtomicInteger countActiveCalls = new AtomicInteger();
        volatile Runnable onCountActive = () -> {};
        private volatile long lastProduced;

        FakeQueue(int active) {
            this.active = active;
        }

        @Override
        public int countActive() {
            countActiveCalls.incrementAndGet();
            onCountActive.run();
            return active;
        }

        @Override
        public long getBlockedUntil() {
            return -1;
        }

        @Override
        public int getDelay() {
            return 0;
        }

        @Override
        public void setLastProduced(long lastProduced) {
            this.lastProduced = lastProduced;
        }

        @Override
        public long getLastProduced() {
            return lastProduced;
        }

        @Override
        public int getInProcess(long now) {
            return 0;
        }

        @Override
        public int getCountCompleted() {
            return 0;
        }

        @Override
        public Boolean isLimitReached() {
            return false;
        }

        @Override
        public void setDelay(int delayRequestable) {}

        @Override
        public void setBlockedUntil(long until) {}

        @Override
        public void setCrawlLimit(int crawlLimit) {}
    }

    /** Stands in for the gRPC call so that the test can cancel it. */
    static class CancellableObserver extends ServerCallStreamObserver<URLInfo> {
        final CollectingObserver delegate = new CollectingObserver();
        volatile Runnable onCancelHandler;

        void cancel() {
            onCancelHandler.run();
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
            this.onCancelHandler = onCancelHandler;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setOnReadyHandler(Runnable onReadyHandler) {}

        @Override
        public void setCompression(String compression) {}

        @Override
        public void disableAutoInboundFlowControl() {}

        @Override
        public void request(int count) {}

        @Override
        public void setMessageCompression(boolean enable) {}

        @Override
        public void onNext(URLInfo value) {
            delegate.onNext(value);
        }

        @Override
        public void onError(Throwable t) {
            delegate.onError(t);
        }

        @Override
        public void onCompleted() {
            delegate.onCompleted();
        }
    }

    static List<FakeQueue> addFakeQueues(AbstractFrontierService service, int count, int active) {
        List<FakeQueue> queues = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            FakeQueue queue = new FakeQueue(active);
            service.getQueues().put(QueueWithinCrawl.get("fake" + i, "crawl_id"), queue);
            queues.add(queue);
        }
        return queues;
    }

    @Test
    void queuesWithNothingScheduledAreNotSentToTheBackend() throws Exception {
        CountingService service = new CountingService();
        GetURLsReservationTest.seedQueue(service);
        List<FakeQueue> empty = addFakeQueues(service, 50, 0);

        CollectingObserver observer = new CollectingObserver();
        service.getURLs(ROTATION_REQUEST, observer);
        assertTrue(observer.completed.await(5, TimeUnit.SECONDS));

        assertEquals(1, observer.received.get());
        assertEquals(1, service.sends.get(), "empty queues were handed over to the backend");
        // they were all looked at, so the skip is what kept them away from the backend
        for (FakeQueue queue : empty) {
            assertEquals(1, queue.countActiveCalls.get());
            assertEquals(0, queue.getLastProduced(), "an empty queue was reserved");
        }
    }

    @Test
    void stopsLookingForQueuesOnceCancelled() throws Exception {
        CountingService service = new CountingService();
        List<FakeQueue> queues = addFakeQueues(service, 50, 1);
        CancellableObserver call = new CancellableObserver();
        // the client goes away while the first queue is being looked at
        queues.get(0).onCountActive = call::cancel;

        service.getURLs(ROTATION_REQUEST, call);
        assertNotNull(call.onCancelHandler, "getURLs did not register a cancel handler");
        assertTrue(call.delegate.completed.await(5, TimeUnit.SECONDS));

        // the first queue was submitted before the cancellation was noticed, but its task
        // must not ask the backend for URLs
        assertEquals(0, service.sends.get());
        for (FakeQueue queue : queues.subList(1, queues.size())) {
            assertEquals(0, queue.countActiveCalls.get(), "kept looking after cancellation");
        }
    }

    @Test
    void stopsLookingForQueuesOnceTokensAreTaken() throws Exception {
        CountingService service = new CountingService();
        GetURLsReservationTest.seedQueue(service);
        List<FakeQueue> queues = addFakeQueues(service, 50, 1);
        // hold the loop on the first fake queue until the real one has taken the only token
        queues.get(0).onCountActive =
                () -> {
                    try {
                        service.firstSendDone.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                };

        GetParams request = ROTATION_REQUEST.toBuilder().setMaxQueues(1).build();
        CollectingObserver observer = new CollectingObserver();
        service.getURLs(request, observer);
        assertTrue(observer.completed.await(5, TimeUnit.SECONDS));

        assertEquals(1, observer.received.get());
        // the first fake queue was submitted before the tokens ran out, but its task must not
        // ask the backend for URLs
        assertEquals(1, service.sends.get());
        for (FakeQueue queue : queues.subList(1, queues.size())) {
            assertEquals(0, queue.countActiveCalls.get(), "kept looking after the last token");
        }
    }
}
