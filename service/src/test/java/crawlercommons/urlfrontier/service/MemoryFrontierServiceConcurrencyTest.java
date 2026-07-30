// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.PurgeUrlParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import crawlercommons.urlfrontier.service.memory.URLQueue;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MemoryFrontierServiceConcurrencyTest {

    private static final String CRAWL_ID = "crawl_id";
    private static final String KEY = "queue_mysite";

    @Test
    void concurrentPutUrlsOnANewKeyCreatesOneQueueAndKeepsBothUrls() throws Exception {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread t1 =
                new Thread(
                        () ->
                                putOneAfterStart(
                                        service,
                                        discovered("https://example.com/a"),
                                        start,
                                        failure));
        Thread t2 =
                new Thread(
                        () ->
                                putOneAfterStart(
                                        service,
                                        discovered("https://example.com/b"),
                                        start,
                                        failure));

        t1.start();
        t2.start();
        start.countDown();
        t1.join();
        t2.join();

        if (failure.get() != null) {
            fail(failure.get());
        }

        assertEquals(1, service.getQueues().size());
        assertEquals(2, ServiceTestUtil.countAllURLs(service));
        URLQueue queue = (URLQueue) service.getQueues().get(QueueWithinCrawl.get(KEY, CRAWL_ID));
        assertEquals(2, queue.countActive());
    }

    @Test
    void knownNeverUrlOnFreshQueueStartsCompletedAndIsNotServed() throws Exception {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        putOne(service, known("https://example.com/completed", 0));

        URLQueue queue = (URLQueue) service.getQueues().get(QueueWithinCrawl.get(KEY, CRAWL_ID));
        assertEquals(0, queue.countActive());
        assertEquals(1, queue.getCountCompleted());

        AtomicInteger received = new AtomicInteger();
        CountDownLatch completed = new CountDownLatch(1);
        service.getURLs(
                GetParams.newBuilder()
                        .setKey(KEY)
                        .setCrawlID(CRAWL_ID)
                        .setMaxUrlsPerQueue(1)
                        .build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.URLInfo value) {
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
                });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(0, received.get());
    }

    @Test
    void urlIteratorSnapshotSurvivesConcurrentQueueMutation() throws Exception {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        for (int i = 0; i < 50; i++) {
            putOne(service, discovered("https://example.com/seed-" + i));
        }

        Map.Entry<QueueWithinCrawl, QueueInterface> entry =
                service.getQueues().entrySet().iterator().next();
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread writer =
                new Thread(
                        () -> {
                            try {
                                start.await();
                                for (int i = 0; i < 200; i++) {
                                    putOne(service, discovered("https://example.com/live-" + i));
                                }
                            } catch (Throwable t) {
                                failure.compareAndSet(null, t);
                            } finally {
                                running.set(false);
                            }
                        });
        writer.start();
        start.countDown();

        try {
            while (running.get()) {
                try (CloseableIterator<URLItem> iterator = service.urlIterator(entry)) {
                    while (iterator.hasNext()) {
                        iterator.next();
                    }
                }
            }
        } catch (Throwable t) {
            failure.compareAndSet(null, t);
        }

        writer.join();
        if (failure.get() != null) {
            fail(failure.get());
        }
    }

    @Test
    void purgeNeverDeletesUrlsRefreshedDuringThePurge() throws Exception {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);

        long future = Instant.now().getEpochSecond() + 3600;
        for (int i = 0; i < 200; i++) {
            putOne(service, known("https://example.com/stale-" + i, future));
        }
        // creation dates have second resolution: make the seeds strictly older than the cutoff
        Thread.sleep(1100);

        int refreshedCount = 20;
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread refresher =
                new Thread(
                        () -> {
                            try {
                                start.await();
                                for (int round = 0; round < 30; round++) {
                                    for (int i = 0; i < refreshedCount; i++) {
                                        putOne(
                                                service,
                                                known("https://example.com/stale-" + i, future));
                                    }
                                }
                            } catch (Throwable t) {
                                failure.compareAndSet(null, t);
                            }
                        });

        Thread purger =
                new Thread(
                        () -> {
                            try {
                                start.await();
                                service.purgeURLs(
                                        PurgeUrlParams.newBuilder()
                                                .setCrawlID(CRAWL_ID)
                                                .setKey(KEY)
                                                .setDays(0)
                                                .build(),
                                        new StreamObserver<>() {
                                            @Override
                                            public void onNext(
                                                    crawlercommons.urlfrontier.Urlfrontier.Long
                                                            value) {}

                                            @Override
                                            public void onError(Throwable t) {
                                                failure.compareAndSet(null, t);
                                            }

                                            @Override
                                            public void onCompleted() {}
                                        });
                            } catch (Throwable t) {
                                failure.compareAndSet(null, t);
                            }
                        });

        refresher.start();
        purger.start();
        start.countDown();
        refresher.join();
        purger.join();

        if (failure.get() != null) {
            fail(failure.get());
        }

        // every refreshed URL was re-put with a creation date at or after the purge cutoff:
        // with scan+delete atomic per queue none of them may end up deleted
        for (int i = 0; i < refreshedCount; i++) {
            assertTrue(
                    urlIsKnown(service, "https://example.com/stale-" + i),
                    "URL refreshed during the purge was deleted: stale-" + i);
        }
    }

    private static boolean urlIsKnown(MemoryFrontierService service, String url) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicBoolean found = new AtomicBoolean(false);
        service.getURLStatus(
                URLStatusRequest.newBuilder().setUrl(url).setKey(KEY).setCrawlID(CRAWL_ID).build(),
                new StreamObserver<>() {
                    @Override
                    public void onNext(URLItem value) {
                        found.set(true);
                    }

                    @Override
                    public void onError(Throwable t) {
                        done.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        done.countDown();
                    }
                });
        assertTrue(done.await(5, TimeUnit.SECONDS));
        return found.get();
    }

    private static URLItem discovered(String url) {
        URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(CRAWL_ID).setKey(KEY).build();
        return URLItem.newBuilder()
                .setID(CRAWL_ID + "_" + url)
                .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                .build();
    }

    private static URLItem known(String url, long nextFetchDate) {
        URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(CRAWL_ID).setKey(KEY).build();
        return URLItem.newBuilder()
                .setID(CRAWL_ID + "_" + url)
                .setKnown(
                        KnownURLItem.newBuilder()
                                .setInfo(info)
                                .setRefetchableFromDate(nextFetchDate)
                                .build())
                .build();
    }

    private static void putOneAfterStart(
            MemoryFrontierService service,
            URLItem item,
            CountDownLatch start,
            AtomicReference<Throwable> failure) {
        try {
            start.await();
            putOne(service, item);
        } catch (Throwable t) {
            failure.compareAndSet(null, t);
        }
    }

    private static void putOne(MemoryFrontierService service, URLItem item) {
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicInteger acked = new AtomicInteger();

        StreamObserver<URLItem> stream =
                service.putURLs(
                        new StreamObserver<>() {
                            @Override
                            public void onNext(AckMessage value) {
                                acked.incrementAndGet();
                            }

                            @Override
                            public void onError(Throwable t) {
                                completed.set(true);
                                fail(t);
                            }

                            @Override
                            public void onCompleted() {
                                completed.set(true);
                            }
                        });

        stream.onNext(item);
        stream.onCompleted();
        ServiceTestUtil.awaitStreamClosed(completed);
        assertEquals(1, acked.get());
    }
}
