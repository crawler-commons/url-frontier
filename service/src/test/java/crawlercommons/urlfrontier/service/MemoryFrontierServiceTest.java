// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.Pagination;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class MemoryFrontierServiceTest {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(MemoryFrontierServiceTest.class);

    static MemoryFrontierService memoryFrontierService;

    @BeforeAll
    static void setup() {
        memoryFrontierService = new MemoryFrontierService("localhost", 7071);
        ServiceTestUtil.initURLs(memoryFrontierService);
    }

    @Test
    void testGetStatusDiscovered() {

        String crawlId = "crawl_id";
        String url = "https://www.mysite.com/discovered";
        String key = "queue_mysite";

        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setKey(key).setUrl(url).build();

        final AtomicInteger discovered = new AtomicInteger(0);
        final AtomicInteger count = new AtomicInteger(0);

        StreamObserver<URLItem> statusObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);

                        if (value.hasKnown()) {
                            discovered.incrementAndGet();
                            assertTrue(value.getKnown().getRefetchableFromDate() > 0);
                        }
                        count.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testGetStatusDiscovered");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, discovered.get());
    }

    @Test
    void testGetStatusCompleted() {

        String crawlId = "crawl_id";
        String url = "https://www.mysite.com/completed";
        String key = "queue_mysite";

        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setKey(key).setUrl(url).build();

        final AtomicInteger fetched = new AtomicInteger(0);
        final AtomicInteger count = new AtomicInteger(0);

        StreamObserver<URLItem> statusObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);
                        if (value.hasKnown()) {
                            fetched.incrementAndGet();
                            assertEquals(0, value.getKnown().getRefetchableFromDate());
                        }
                        count.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testGetStatusKnown");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, fetched.get());
    }

    @Test
    void testNotFound() {
        String crawlId = "crawl_id";
        String url = "https://www.example3.com";
        String key = "queue_3";

        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setKey(key).setUrl(url).build();

        final AtomicInteger count = new AtomicInteger(0);

        StreamObserver<URLItem> statusObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);
                        count.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable t) {
                        assertEquals(io.grpc.Status.NOT_FOUND, io.grpc.Status.fromThrowable(t));
                        LOG.error(t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNotFound");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver);

        assertEquals(0, count.get());
    }

    private void logURLItem(URLItem item) {
        if (item.hasDiscovered()) {
            LOG.info(item.getDiscovered().toString());
        } else if (item.hasKnown()) {
            LOG.info(item.getKnown().toString());
        } else {
            LOG.error("Unknown URLItem type");
        }
    }

    @Test
    void testGetStatusToRefetch() {

        String crawlId = "crawl_id";
        String url = "https://www.mysite.com/knowntorefetch";
        String key = "queue_mysite";

        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setKey(key).setUrl(url).build();

        final AtomicInteger fetched = new AtomicInteger(0);
        final AtomicInteger count = new AtomicInteger(0);

        StreamObserver<URLItem> statusObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);

                        // Internally, MemoryFrontierService does not make a distinction
                        // between discovered and known which have to be re-fetched
                        if (value.hasKnown()) {
                            fetched.incrementAndGet();
                            assertTrue(value.getKnown().getRefetchableFromDate() > 0);
                        }
                        count.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testGetStatusKnown");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, fetched.get());
    }

    @Test
    void testListURLs() {

        Pagination pagination = Pagination.newBuilder().setCrawlID("crawl_id").build();

        final AtomicInteger fetched = new AtomicInteger(0);
        final AtomicInteger count = new AtomicInteger(0);

        StreamObserver<URLItem> statusObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);

                        if (value.hasKnown()) {
                            fetched.incrementAndGet();
                        }
                        count.incrementAndGet();
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testListURLs");
                    }
                };

        memoryFrontierService.listURLs(pagination, statusObserver);
        assertEquals(3, count.get());
    }
}
