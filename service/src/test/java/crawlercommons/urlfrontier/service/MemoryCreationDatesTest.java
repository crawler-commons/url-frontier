// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Creation dates are scoped to the queue they belong to, see issue #166. */
class MemoryCreationDatesTest {

    private static final String URL = "https://www.mysite.com/shared";
    private static final String KEY = "queue_mysite";

    private MemoryFrontierService service;

    @BeforeEach
    void setup() {
        service = new MemoryFrontierService("localhost", 7071);
    }

    @AfterEach
    void tearDown() throws Exception {
        service.close();
    }

    @Test
    void sameURLInTwoCrawlsKeepsItsOwnCreationDate() {
        putDiscovered("crawlA", KEY, URL);
        putDiscovered("crawlB", KEY, URL);

        deleteKnown("crawlA", KEY, URL);

        // the URL is gone from crawlA but crawlB still knows when it was created
        assertTrue(creationDate("crawlB", KEY, URL) > 0, "creation date lost for the other crawl");
    }

    @Test
    void sameURLInTwoQueuesKeepsItsOwnCreationDate() {
        putDiscovered("crawlA", KEY, URL);
        putDiscovered("crawlA", "another_queue", URL);

        deleteKnown("crawlA", KEY, URL);

        assertTrue(
                creationDate("crawlA", "another_queue", URL) > 0,
                "creation date lost for the other queue");
    }

    @Test
    void updatingAURLKeepsItsOriginalCreationDate() throws InterruptedException {
        putDiscovered("crawlA", KEY, URL);
        long initial = creationDate("crawlA", KEY, URL);

        // the dates have a resolution of a second: make sure a refreshed one would differ
        Thread.sleep(1100);

        putKnown("crawlA", KEY, URL, Instant.now().getEpochSecond() + 3600);

        assertEquals(initial, creationDate("crawlA", KEY, URL), "creation date was refreshed");
    }

    @Test
    void completingAURLKeepsItsOriginalCreationDate() throws InterruptedException {
        putDiscovered("crawlA", KEY, URL);
        long initial = creationDate("crawlA", KEY, URL);

        Thread.sleep(1100);

        // a nextFetchDate of 0 moves the URL to the completed set
        putKnown("crawlA", KEY, URL, 0);

        assertEquals(initial, creationDate("crawlA", KEY, URL), "creation date was refreshed");
    }

    private long creationDate(String crawlID, String key, String url) {
        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlID).setKey(key).setUrl(url).build();

        final AtomicLong date = new AtomicLong(-1);

        service.getURLStatus(
                request,
                new StreamObserver<>() {
                    @Override
                    public void onNext(URLItem value) {
                        date.set(value.getCreationDate());
                    }

                    @Override
                    public void onError(Throwable t) {
                        fail("getURLStatus failed for " + url + " in " + crawlID, t);
                    }

                    @Override
                    public void onCompleted() {}
                });

        assertTrue(date.get() >= 0, "no status returned for " + url + " in " + crawlID);
        return date.get();
    }

    private void putDiscovered(String crawlID, String key, String url) {
        URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(crawlID).setKey(key).build();
        put(
                URLItem.newBuilder()
                        .setID(crawlID + "_" + url)
                        .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                        .build());
    }

    private void putKnown(String crawlID, String key, String url, long refetchableFromDate) {
        put(knownItem(crawlID, key, url, refetchableFromDate));
    }

    private void deleteKnown(String crawlID, String key, String url) {
        service.deleteURLItem(knownItem(crawlID, key, url, Instant.now().getEpochSecond()));
    }

    private URLItem knownItem(String crawlID, String key, String url, long refetchableFromDate) {
        URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(crawlID).setKey(key).build();
        return URLItem.newBuilder()
                .setID(crawlID + "_" + url)
                .setKnown(
                        KnownURLItem.newBuilder()
                                .setInfo(info)
                                .setRefetchableFromDate(refetchableFromDate)
                                .build())
                .build();
    }

    private void put(URLItem item) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        StreamObserver<URLItem> requestObserver =
                service.putURLs(
                        new StreamObserver<>() {
                            @Override
                            public void onNext(AckMessage value) {
                                assertEquals(AckMessage.Status.OK, value.getStatus());
                                acked.incrementAndGet();
                            }

                            @Override
                            public void onError(Throwable t) {
                                completed.set(true);
                                fail("putURLs failed", t);
                            }

                            @Override
                            public void onCompleted() {
                                completed.set(true);
                            }
                        });

        requestObserver.onNext(item);
        requestObserver.onCompleted();

        ServiceTestUtil.awaitStreamClosed(completed);
        assertEquals(1, acked.get());
    }
}
