// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.Pagination;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The offset of ListQueues is a position among the queues matching the request, not among the
 * entries of the queue map: a client paging through a crawl must see every one of its queues
 * exactly once, whatever else the frontier holds.
 */
class ListQueuesPaginationTest {

    private static final String CRAWL_A = "crawl_a";
    private static final String CRAWL_B = "crawl_b";
    private static final int QUEUES_PER_CRAWL = 6;

    private MemoryFrontierService service;

    @BeforeEach
    void setup() {
        service = new MemoryFrontierService("localhost", 0);
    }

    @AfterEach
    void tearDown() throws Exception {
        service.close();
    }

    @Test
    void pagesOverOneCrawlWhenAnotherIsInterleaved() {
        // the two crawls alternate in the queue map, so an offset counting raw entries
        // would land halfway through the other crawl
        for (int i = 0; i < QUEUES_PER_CRAWL; i++) {
            discover(CRAWL_A, "queue_a" + i, "https://a" + i + ".example.com/");
            discover(CRAWL_B, "queue_b" + i, "https://b" + i + ".example.com/");
        }

        assertEquals(
                QUEUES_PER_CRAWL * 2, service.getQueues().size(), "queues created by the fixture");

        final Set<String> seen = pageThrough(CRAWL_A, 2);

        assertEquals(QUEUES_PER_CRAWL, seen.size(), "every queue of the crawl, none twice");
        for (int i = 0; i < QUEUES_PER_CRAWL; i++) {
            assertTrue(seen.contains("queue_a" + i), "missing queue_a" + i);
        }
    }

    @Test
    void inactiveQueuesDoNotShiftTheOffset() {
        // one queue out of two has all its URLs completed, so it is filtered out unless
        // include_inactive is set
        for (int i = 0; i < QUEUES_PER_CRAWL; i++) {
            final String key = "queue_a" + i;
            final String url = "https://a" + i + ".example.com/";
            discover(CRAWL_A, key, url);
            if (i % 2 == 1) {
                complete(CRAWL_A, key, url);
            }
        }

        final Set<String> active = pageThrough(CRAWL_A, 1);

        assertEquals(QUEUES_PER_CRAWL / 2, active.size(), "only the queues with active URLs");
        for (int i = 0; i < QUEUES_PER_CRAWL; i += 2) {
            assertTrue(active.contains("queue_a" + i), "missing queue_a" + i);
        }

        assertEquals(
                QUEUES_PER_CRAWL,
                pageThrough(CRAWL_A, 2, true).size(),
                "every queue when the inactive ones are included");
    }

    @Test
    void reportsThePaginationBackToTheClient() {
        for (int i = 0; i < QUEUES_PER_CRAWL; i++) {
            discover(CRAWL_A, "queue_a" + i, "https://a" + i + ".example.com/");
        }

        final QueueList page = listQueues(CRAWL_A, 2, 4, false);

        assertEquals(2, page.getValuesCount(), "values returned");
        assertEquals(2, page.getSize(), "size reported");
        assertEquals(4, page.getStart(), "start echoed back");
        assertEquals(CRAWL_A, page.getCrawlID(), "crawl ID reported");
    }

    /** Pages through a crawl the way the client does, and returns the distinct queues seen. */
    private Set<String> pageThrough(String crawlID, int pageSize) {
        return pageThrough(crawlID, pageSize, false);
    }

    private Set<String> pageThrough(String crawlID, int pageSize, boolean includeInactive) {
        final Set<String> seen = new LinkedHashSet<>();
        int totalReturned = 0;
        int start = 0;
        while (true) {
            final QueueList page = listQueues(crawlID, pageSize, start, includeInactive);
            if (page.getValuesCount() == 0) {
                break;
            }
            seen.addAll(page.getValuesList());
            totalReturned += page.getValuesCount();
            start += page.getValuesCount();
        }
        assertEquals(seen.size(), totalReturned, "no queue returned in two different pages");
        return seen;
    }

    private QueueList listQueues(String crawlID, int size, int start, boolean includeInactive) {
        final List<QueueList> received = new ArrayList<>();
        service.listQueues(
                Pagination.newBuilder()
                        .setCrawlID(crawlID)
                        .setSize(size)
                        .setStart(start)
                        .setIncludeInactive(includeInactive)
                        .setLocal(true)
                        .build(),
                collectingObserver(received));
        assertEquals(1, received.size(), "one response per listQueues call");
        return received.get(0);
    }

    private void discover(String crawlID, String key, String url) {
        final URLInfo info =
                URLInfo.newBuilder().setUrl(url).setCrawlID(crawlID).setKey(key).build();
        send(
                URLItem.newBuilder()
                        .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                        .setID(crawlID + "_" + url)
                        .build());
    }

    /** Marks the URL as done, which leaves its queue with no active URL. */
    private void complete(String crawlID, String key, String url) {
        final URLInfo info =
                URLInfo.newBuilder().setUrl(url).setCrawlID(crawlID).setKey(key).build();
        send(
                URLItem.newBuilder()
                        .setKnown(
                                KnownURLItem.newBuilder()
                                        .setInfo(info)
                                        .setRefetchableFromDate(0)
                                        .build())
                        .setID(crawlID + "_" + url)
                        .build());
    }

    private void send(URLItem item) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        final StreamObserver<URLItem> stream =
                service.putURLs(
                        new StreamObserver<>() {
                            @Override
                            public void onNext(AckMessage value) {
                                if (value.getStatus() == AckMessage.Status.FAIL) {
                                    fail("the frontier failed to store " + value.getID());
                                }
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
        assertEquals(1, acked.get(), "the item was acked");
    }

    private static StreamObserver<QueueList> collectingObserver(List<QueueList> received) {
        return new StreamObserver<>() {
            @Override
            public void onNext(QueueList value) {
                received.add(value);
            }

            @Override
            public void onError(Throwable t) {
                fail(t);
            }

            @Override
            public void onCompleted() {}
        };
    }
}
