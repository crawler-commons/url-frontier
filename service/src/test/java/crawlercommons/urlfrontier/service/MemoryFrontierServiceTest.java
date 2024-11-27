// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @Order(1)
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
    @Order(2)
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
                        LOG.info("completed testGetStatusCompleted");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, fetched.get());
    }

    @Test
    @Order(3)
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
    @Order(4)
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
                        LOG.info("completed testGetStatusToRefetch");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, fetched.get());
    }

    @Test
    @Order(5)
    void testListAllURLs() {

        ListUrlParams params =
                ListUrlParams.newBuilder().setCrawlID("crawl_id").setStart(0).setSize(100).build();

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
                        LOG.info("completed testListAllURLs");
                    }
                };

        memoryFrontierService.listURLs(params, statusObserver);
        assertEquals(4, count.get());
    }

    @Test
    @Order(6)
    void testListURLsinglequeue() {

        ListUrlParams params =
                ListUrlParams.newBuilder()
                        .setCrawlID("crawl_id")
                        .setKey("another_queue")
                        .setSize(100)
                        .build();

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

        memoryFrontierService.listURLs(params, statusObserver);
        assertEquals(1, count.get());
    }

    @Test
    @Order(7)
    void testMemoryIterator() {
        int nbQueues = 0;
        int nbUrls = 0;

        for (Entry<QueueWithinCrawl, QueueInterface> cur :
                memoryFrontierService.getQueues().entrySet()) {
            nbQueues++;
            System.out.println("Queue: " + cur.getKey());
            Iterator<URLItem> iter = memoryFrontierService.urlIterator(cur, 0, 100);
            while (iter.hasNext()) {
                URLItem item = iter.next();
                System.out.println(item.toString());
                nbUrls++;
            }
        }

        assertEquals(2, nbQueues);
        assertEquals(4, nbUrls);
    }

    @Test
    @Order(8)
    void testMemoryIteratorSingleQueue() {
        int nbQueues = 0;
        int nbUrls = 0;

        for (Entry<QueueWithinCrawl, QueueInterface> cur :
                memoryFrontierService.getQueues().entrySet()) {
            if (cur.getKey().getQueue().equals("another_queue")) {
                continue;
            }

            nbQueues++;
            System.out.println("Queue: " + cur.getKey());
            Iterator<URLItem> iter = memoryFrontierService.urlIterator(cur, 0, 100);
            while (iter.hasNext()) {
                URLItem item = iter.next();
                System.out.println(item.toString());
                nbUrls++;
            }
        }

        assertEquals(1, nbQueues);
        assertEquals(3, nbUrls);
    }

    @Test
    @Order(9)
    void testListAllURLsCaseInsensitive() {

        ListUrlParams params =
                ListUrlParams.newBuilder()
                        .setCrawlID("crawl_id")
                        .setStart(0)
                        .setSize(100)
                        .setFilter("COMPLETED")
                        .setIgnoreCase(true)
                        .build();

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
                        LOG.info("completed testListAllURLsCaseInsensitive");
                    }
                };

        memoryFrontierService.listURLs(params, statusObserver);
        assertEquals(1, count.get());
    }

    @Test
    @Order(10)
    void testListAllURLsCaseSensitive() {

        ListUrlParams params =
                ListUrlParams.newBuilder()
                        .setCrawlID("crawl_id")
                        .setStart(0)
                        .setSize(100)
                        .setFilter("COMPLETED")
                        .setIgnoreCase(false)
                        .build();

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
                        LOG.info("completed testListAllURLsCaseSensitive");
                    }
                };

        memoryFrontierService.listURLs(params, statusObserver);
        assertEquals(0, count.get());
    }

    @Test
    @Order(11)
    void testListAllURLstart() {

        ListUrlParams params =
                ListUrlParams.newBuilder().setCrawlID("crawl_id").setStart(3).setSize(10).build();

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
                        LOG.info("completed testListAllURLs");
                    }
                };

        memoryFrontierService.listURLs(params, statusObserver);
        assertEquals(1, count.get());
    }

    @Test
    @Order(99)
    // Must be last test
    void testNoRescheduleCompleted() {

        String crawlId = "crawl_id";
        String url2 = "https://www.mysite.com/completed";
        String key2 = "queue_mysite";
        StringList sl2 = StringList.newBuilder().addValues("md2").build();

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder1 = URLItem.newBuilder();

        StreamObserver<URLItem> statusObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);

                        // Internally, MemoryFrontierService does not make a distinction
                        // between discovered and known which have to be re-fetched
                        if (value.hasKnown()) {
                            assertEquals(0, value.getKnown().getRefetchableFromDate());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 1/2");
                    }
                };

        // First check that we have the URL as Known URL with a refetch date of 0
        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setUrl(url2).setKey(key2).build();

        memoryFrontierService.getURLStatus(request, statusObserver);

        // PutURL for the same URL with Discovered status
        URLInfo info2 =
                URLInfo.newBuilder()
                        .setUrl(url2)
                        .setCrawlID(crawlId)
                        .setKey(key2)
                        .putMetadata("meta1", sl2)
                        .build();

        DiscoveredURLItem disco2 = DiscoveredURLItem.newBuilder().setInfo(info2).build();
        builder1.clear();
        builder1.setDiscovered(disco2);
        builder1.setID(crawlId + "_" + url2);

        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);
        final AtomicInteger skipped = new AtomicInteger(0);
        final AtomicInteger ok = new AtomicInteger(0);
        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.AckMessage value) {
                        // receives confirmation that the value has been received
                        acked.addAndGet(1);
                        if (value.getStatus().equals(AckMessage.Status.SKIPPED)) {
                            skipped.getAndIncrement();
                            LOG.info("PutURL skipped");
                        } else if (value.getStatus().equals(AckMessage.Status.FAIL)) {
                            failed.getAndIncrement();
                            LOG.info("PutURL failed");
                        } else if (value.getStatus().equals(AckMessage.Status.OK)) {
                            ok.getAndIncrement();
                            LOG.info("PutURL OK");
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        completed.set(true);
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        completed.set(true);
                        LOG.info("Completed putURL");
                    }
                };

        StreamObserver<URLItem> streamObserver = memoryFrontierService.putURLs(responseObserver);
        streamObserver.onNext(builder1.build());
        streamObserver.onCompleted();

        assertEquals(1, skipped.get());

        StreamObserver<URLItem> statusObserver2 =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);

                        // Internally, MemoryFrontierService does not make a distinction
                        // between discovered and known which have to be re-fetched
                        if (value.hasKnown()) {
                            assertEquals(0, value.getKnown().getRefetchableFromDate());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 2/2");
                    }
                };

        memoryFrontierService.getURLStatus(request, statusObserver2);
    }
}
