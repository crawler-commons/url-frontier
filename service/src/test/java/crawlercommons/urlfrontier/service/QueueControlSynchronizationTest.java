// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

/**
 * The control RPCs must update queue state under the queue monitor — the same lock the reservation
 * gate uses (#152).
 */
class QueueControlSynchronizationTest {

    /** Records whether each setter was invoked while holding this queue's monitor. */
    static class LockRecordingQueue implements QueueInterface {
        volatile boolean delayLocked;
        volatile boolean blockedLocked;
        volatile boolean limitLocked;

        @Override
        public void setDelay(int delayRequestable) {
            delayLocked = Thread.holdsLock(this);
        }

        @Override
        public void setBlockedUntil(long until) {
            blockedLocked = Thread.holdsLock(this);
        }

        @Override
        public void setCrawlLimit(int crawlLimit) {
            limitLocked = Thread.holdsLock(this);
        }

        @Override
        public long getBlockedUntil() {
            return -1;
        }

        @Override
        public int getDelay() {
            return -1;
        }

        @Override
        public void setLastProduced(long lastProduced) {}

        @Override
        public long getLastProduced() {
            return 0;
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
        public int countActive() {
            return 0;
        }

        @Override
        public Boolean isLimitReached() {
            return false;
        }
    }

    static class NoopEmptyObserver implements StreamObserver<Empty> {
        @Override
        public void onNext(Empty value) {}

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    }

    @Test
    void controlUpdatesHoldTheQueueMonitor() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        LockRecordingQueue queue = new LockRecordingQueue();
        QueueWithinCrawl qwc = QueueWithinCrawl.get("lockq", "crawl_id");
        service.getQueues().put(qwc, queue);

        service.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey("lockq")
                        .setCrawlID("crawl_id")
                        .setDelayRequestable(7)
                        .build(),
                new NoopEmptyObserver());
        service.blockQueueUntil(
                BlockQueueParams.newBuilder()
                        .setKey("lockq")
                        .setCrawlID("crawl_id")
                        .setTime(9999999999L)
                        .build(),
                new NoopEmptyObserver());
        service.setCrawlLimit(
                CrawlLimitParams.newBuilder()
                        .setKey("lockq")
                        .setCrawlID("crawl_id")
                        .setLimit(5)
                        .build(),
                new NoopEmptyObserver());

        assertTrue(queue.delayLocked, "setDelay must hold the queue monitor");
        assertTrue(queue.blockedLocked, "blockQueueUntil must hold the queue monitor");
        assertTrue(queue.limitLocked, "setCrawlLimit must hold the queue monitor");
    }
}
