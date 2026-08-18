// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.SynchronizedStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Reproduces issue #162: {@link java.util.PriorityQueue#iterator()} walks the backing heap array
 * and is not in sort order, so neither sendURLsForQueue nor getInProcess can stop at the first
 * element that does not match.
 */
class MemoryQueueOrderingTest {

    static final String CRAWL_ID = "crawl_id";
    static final String KEY = "queue_mysite";
    static final QueueWithinCrawl QWC = QueueWithinCrawl.get(KEY, CRAWL_ID);

    static final long NOW = 1_000_000L;

    static class CollectingObserver implements StreamObserver<URLInfo> {
        final List<String> received = new ArrayList<>();

        @Override
        public void onNext(URLInfo value) {
            received.add(value.getUrl());
        }

        @Override
        public void onError(Throwable t) {}

        @Override
        public void onCompleted() {}
    }

    private static InternalURL known(String url, long refetchableFromDate) {
        URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(CRAWL_ID).setKey(KEY).build();
        URLItem item =
                URLItem.newBuilder()
                        .setID(CRAWL_ID + "_" + url)
                        .setKnown(
                                KnownURLItem.newBuilder()
                                        .setInfo(info)
                                        .setRefetchableFromDate(refetchableFromDate)
                                        .build())
                        .build();
        return (InternalURL) InternalURL.from(item)[2];
    }

    /**
     * Five URLs whose insertion order leaves the heap array as [-100, +100, -50, +400, +300]
     * relative to {@link #NOW}: the second element is not due yet but the third one is, so anything
     * stopping at the first non-matching element misses it.
     */
    private static URLQueue interleavedQueue() {
        URLQueue queue = new URLQueue(known("https://www.mysite.com/a", NOW - 100));
        queue.add(known("https://www.mysite.com/b", NOW + 300));
        queue.add(known("https://www.mysite.com/c", NOW - 50));
        queue.add(known("https://www.mysite.com/d", NOW + 400));
        queue.add(known("https://www.mysite.com/e", NOW + 100));
        return queue;
    }

    @SuppressWarnings("unchecked")
    private static SynchronizedStreamObserver<URLInfo> wrap(StreamObserver<URLInfo> observer) {
        // -1 token means the queue gate is deactivated
        return (SynchronizedStreamObserver<URLInfo>)
                SynchronizedStreamObserver.wrapping(observer, -1);
    }

    @Test
    void dueURLsBehindANotDueOneAreSent() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        URLQueue queue = interleavedQueue();

        CollectingObserver observer = new CollectingObserver();
        int sent = service.sendURLsForQueue(queue, QWC, 10, 30, NOW, wrap(observer));

        assertEquals(2, sent, "a due URL was skipped because of the heap iteration order");
        assertEquals(
                List.of("https://www.mysite.com/a", "https://www.mysite.com/c"),
                observer.received,
                "due URLs must be sent oldest first");
    }

    @Test
    void notDueURLsAreNotSent() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        URLQueue queue = interleavedQueue();

        CollectingObserver observer = new CollectingObserver();
        // 200 secs before the earliest nextFetchDate
        int sent = service.sendURLsForQueue(queue, QWC, 10, 30, NOW - 200, wrap(observer));

        assertEquals(0, sent);
        assertEquals(List.of(), observer.received);
    }

    @Test
    void maxURLsPerQueueTakesTheOldestFirst() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        URLQueue queue = interleavedQueue();

        CollectingObserver observer = new CollectingObserver();
        // everything is due, but only 2 can be sent
        int sent = service.sendURLsForQueue(queue, QWC, 2, 30, NOW + 1000, wrap(observer));

        assertEquals(2, sent);
        assertEquals(
                List.of("https://www.mysite.com/a", "https://www.mysite.com/c"), observer.received);
    }

    @Test
    void sendingMarksURLsAsInProcess() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        URLQueue queue = interleavedQueue();

        assertEquals(0, queue.getInProcess(NOW));

        service.sendURLsForQueue(queue, QWC, 10, 30, NOW, wrap(new CollectingObserver()));

        assertEquals(2, queue.getInProcess(NOW));
        // the hold has expired
        assertEquals(0, queue.getInProcess(NOW + 30));

        // and they are not sent again while held
        CollectingObserver second = new CollectingObserver();
        assertEquals(0, service.sendURLsForQueue(queue, QWC, 10, 30, NOW, wrap(second)));
    }

    /** Ten URLs, all due at {@link #NOW}, added in an order that does not match their dates. */
    private static URLQueue allDueQueue() {
        URLQueue queue = new URLQueue(known("https://www.mysite.com/url-5", NOW - 50));
        for (int i = 0; i < 10; i++) {
            if (i != 5) {
                queue.add(known("https://www.mysite.com/url-" + i, NOW - 100 + i * 10));
            }
        }
        return queue;
    }

    private static List<String> sortedURLs(URLQueue queue) {
        List<InternalURL> content = new ArrayList<>(queue);
        content.sort(null);
        List<String> urls = new ArrayList<>();
        for (InternalURL iu : content) {
            urls.add(iu.url);
        }
        return urls;
    }

    @Test
    void sendingLeavesTheQueueUnchanged() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        URLQueue queue = allDueQueue();

        List<String> before = sortedURLs(queue);

        // fewer than the queue holds, so the selection stops part way through it
        assertEquals(
                3,
                service.sendURLsForQueue(queue, QWC, 3, 30, NOW, wrap(new CollectingObserver())));

        assertEquals(
                10, queue.countActive(), "polling the selection out must not shrink the queue");
        assertEquals(before, sortedURLs(queue), "the queue must hold the same URLs as before");
        assertEquals(
                "https://www.mysite.com/url-0",
                queue.peek().url,
                "the head must still be the oldest URL");
    }

    @Test
    void dueURLsBehindHeldOnesAreSent() {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
        URLQueue queue = allDueQueue();

        // hold the three oldest, which sit at the front of the sort order
        for (InternalURL iu : queue) {
            if (iu.url.endsWith("-0") || iu.url.endsWith("-1") || iu.url.endsWith("-2")) {
                iu.setHeldUntil(NOW + 60);
            }
        }

        CollectingObserver observer = new CollectingObserver();
        int sent = service.sendURLsForQueue(queue, QWC, 2, 30, NOW, wrap(observer));

        assertEquals(2, sent);
        assertEquals(
                List.of("https://www.mysite.com/url-3", "https://www.mysite.com/url-4"),
                observer.received,
                "the selection must poll past the held URLs to the due ones behind them");
        assertEquals(10, queue.countActive());
    }

    @Test
    void inProcessCountsHeldURLsAnywhereInTheHeap() {
        URLQueue queue = interleavedQueue();

        // hold the two entries sitting behind a non-held one in the heap array
        for (InternalURL iu : queue) {
            if (iu.url.endsWith("/c") || iu.url.endsWith("/d")) {
                iu.setHeldUntil(NOW + 60);
            }
        }

        assertEquals(
                2,
                queue.getInProcess(NOW),
                "in-process URLs were missed because of the heap iteration order");
        assertEquals(0, queue.getInProcess(NOW + 60), "expired holds must not count");
    }
}
