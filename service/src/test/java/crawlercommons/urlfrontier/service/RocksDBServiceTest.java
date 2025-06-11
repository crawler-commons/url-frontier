// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.rocksdb.RocksDBService;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RocksDBServiceTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RocksDBServiceTest.class);

    private static final String ROCKSDB_PATH = "./target/rocksdb";

    RocksDBService rocksDBService;

    @AfterEach
    void shutdown() throws IOException {
        rocksDBService.close();
    }

    @AfterAll
    static void cleanup() {
        LOG.info("Cleaning up directory {}", ROCKSDB_PATH);
        FileUtils.deleteQuietly(new File(ROCKSDB_PATH));
    }

    @BeforeEach
    void setup() {

        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", ROCKSDB_PATH);
        rocksDBService = new RocksDBService(conf, "localhost", 7071);
        ServiceTestUtil.initURLs(rocksDBService);
    }

    @Test
    @Order(1)
    void testDiscovered() {
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

                            assertEquals(url, value.getKnown().getInfo().getUrl());
                            assertEquals(crawlId, value.getKnown().getInfo().getCrawlID());
                            assertEquals(key, value.getKnown().getInfo().getKey());
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

        rocksDBService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, discovered.get());
    }

    @Test
    @Order(2)
    void testCompleted() {

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
                            assertEquals(url, value.getKnown().getInfo().getUrl());
                            assertEquals(crawlId, value.getKnown().getInfo().getCrawlID());
                            assertEquals(key, value.getKnown().getInfo().getKey());
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

        rocksDBService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, fetched.get());
    }

    @Test
    @Order(3)
    void testNotFound() {
        String crawlId = "crawl_id";
        String url = "https://www.example3.com";
        String key = "queue_mysite";

        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setKey(key).setUrl(url).build();

        final AtomicInteger count = new AtomicInteger(0);
        final AtomicInteger notfound = new AtomicInteger(0);

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
                        notfound.incrementAndGet();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNotFound");
                    }
                };

        rocksDBService.getURLStatus(request, statusObserver);

        assertEquals(0, count.get());
        assertEquals(1, notfound.get());
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
                            assertTrue(value.getKnown().getRefetchableFromDate() > 0);
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
                        LOG.info("completed testGetStatusKnown");
                    }
                };

        rocksDBService.getURLStatus(request, statusObserver);

        assertEquals(1, count.get());
        assertEquals(1, fetched.get());
    }

    @Test
    @Order(5)
    void testListAllURLs() {

        ListUrlParams params =
                ListUrlParams.newBuilder().setCrawlID("crawl_id").setSize(100).build();

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

        rocksDBService.listURLs(params, statusObserver);
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

        rocksDBService.listURLs(params, statusObserver);
        assertEquals(1, count.get());
    }

    @Test
    @Order(7)
    void testMemoryIterator() {
        int nbQueues = 0;
        int nbUrls = 0;

        for (Entry<QueueWithinCrawl, QueueInterface> cur : rocksDBService.getQueues().entrySet()) {
            nbQueues++;
            System.out.println("Queue: " + cur.getKey());
            Iterator<URLItem> iter = rocksDBService.urlIterator(cur, 0, 100);
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

        for (Entry<QueueWithinCrawl, QueueInterface> cur : rocksDBService.getQueues().entrySet()) {
            if (cur.getKey().getQueue().equals("another_queue")) {
                continue;
            }

            nbQueues++;
            System.out.println("Queue: " + cur.getKey());
            Iterator<URLItem> iter = rocksDBService.urlIterator(cur, 0, 100);
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
    void testCountURLs() {

        Urlfrontier.CountUrlParams.Builder builder = Urlfrontier.CountUrlParams.newBuilder();

        builder.setKey("queue_mysite");
        builder.setCrawlID("crawl_id");
        builder.setLocal(false);

        StreamObserver<Urlfrontier.Long> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(Urlfrontier.Long value) {
                        // receives confirmation that the value has been received
                        assertEquals(3, value.getValue());
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 1/2");
                    }
                };

        rocksDBService.countURLs(builder.build(), responseObserver);
    }

    @Test
    @Order(10)
    void testCountURLsCaseSensitive() {

        Urlfrontier.CountUrlParams.Builder builder = Urlfrontier.CountUrlParams.newBuilder();

        builder.setKey("queue_mysite");
        builder.setCrawlID("crawl_id");
        builder.setFilter("COMPLETED");
        builder.setIgnoreCase(false);

        StreamObserver<Urlfrontier.Long> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(Urlfrontier.Long value) {
                        // receives confirmation that the value has been received
                        assertEquals(0, value.getValue());
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 1/2");
                    }
                };

        rocksDBService.countURLs(builder.build(), responseObserver);
    }

    @Test
    @Order(9)
    void testCountURsLCaseInsensitive() {

        Urlfrontier.CountUrlParams.Builder builder = Urlfrontier.CountUrlParams.newBuilder();

        builder.setKey("queue_mysite");
        builder.setCrawlID("crawl_id");
        builder.setFilter("COMPLETED");
        builder.setIgnoreCase(true);

        StreamObserver<Urlfrontier.Long> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(Urlfrontier.Long value) {
                        // receives confirmation that the value has been received
                        assertEquals(1, value.getValue());
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 1/2");
                    }
                };

        rocksDBService.countURLs(builder.build(), responseObserver);
    }

    @Test
    @Order(98)
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
                        } else {
                            fail();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 1/2");
                    }
                };

        // First check that we have the URL as Known URL with a refetch date of 0
        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setUrl(url2).setKey(key2).build();

        rocksDBService.getURLStatus(request, statusObserver);

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
                            LOG.info("PutURL fail");
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

        StreamObserver<URLItem> streamObserver = rocksDBService.putURLs(responseObserver);
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

        rocksDBService.getURLStatus(request, statusObserver2);
    }

    @Test
    @Order(99)
    void testRescheduleCompleted() {

        String crawlId = "crawl_id";
        String url2 = "https://www.mysite.com/knowntorefetch";
        String key2 = "queue_mysite";
        StringList sl2 = StringList.newBuilder().addValues("md2").build();
        final long refetchDate = 2943003600L;

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
                            long refetch = value.getKnown().getRefetchableFromDate();
                            assertNotEquals(0, refetch);
                            LOG.info("Current refetch date for known URL {}", refetch);
                        } else {
                            fail();
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }

                    @Override
                    public void onCompleted() {
                        LOG.info("completed testNoRescheduleCompleted 1/2");
                    }
                };

        // First check that we have the URL as Known URL with a refetch date <> 0
        URLStatusRequest request =
                URLStatusRequest.newBuilder().setCrawlID(crawlId).setUrl(url2).setKey(key2).build();

        rocksDBService.getURLStatus(request, statusObserver);

        // PutURL for the same URL with Discovered status
        URLInfo info2 =
                URLInfo.newBuilder()
                        .setUrl(url2)
                        .setCrawlID(crawlId)
                        .setKey(key2)
                        .putMetadata("updated_disco", sl2)
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
                            LOG.info("PutURL fail");
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

        StreamObserver<URLItem> streamObserver = rocksDBService.putURLs(responseObserver);
        streamObserver.onNext(builder1.build());
        streamObserver.onCompleted();

        // Verify it has been skipped
        // Once a URL is "known", it can't be set back to "discovered"
        assertEquals(1, skipped.get());

        // Now, try to update the next refetch date for the known URL
        // PutURL for the same URL with Known status and a different refetch date
        URLInfo info3 =
                URLInfo.newBuilder()
                        .setUrl(url2)
                        .setCrawlID(crawlId)
                        .setKey(key2)
                        .putMetadata("updated_known", sl2)
                        .build();

        KnownURLItem update =
                KnownURLItem.newBuilder()
                        .setInfo(info3)
                        .setRefetchableFromDate(refetchDate)
                        .build();
        builder1.clear();
        builder1.setKnown(update);
        builder1.setID(crawlId + "_" + url2);

        final AtomicBoolean completed2 = new AtomicBoolean(false);
        final AtomicInteger acked2 = new AtomicInteger(0);
        final AtomicInteger failed2 = new AtomicInteger(0);
        final AtomicInteger skipped2 = new AtomicInteger(0);
        final AtomicInteger ok2 = new AtomicInteger(0);
        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage> responseObserver2 =
                new StreamObserver<>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.AckMessage value) {
                        // receives confirmation that the value has been received
                        acked2.addAndGet(1);
                        if (value.getStatus().equals(AckMessage.Status.SKIPPED)) {
                            skipped2.getAndIncrement();
                            LOG.info("PutURL skipped");
                        } else if (value.getStatus().equals(AckMessage.Status.FAIL)) {
                            failed2.getAndIncrement();
                            LOG.info("PutURL fail");
                        } else if (value.getStatus().equals(AckMessage.Status.OK)) {
                            ok2.getAndIncrement();
                            LOG.info("PutURL OK");
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        completed2.set(true);
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        completed2.set(true);
                        LOG.info("Completed putURL");
                    }
                };

        StreamObserver<URLItem> streamObserver2 = rocksDBService.putURLs(responseObserver2);
        streamObserver2.onNext(builder1.build());
        streamObserver2.onCompleted();

        assertEquals(1, ok2.get());

        StreamObserver<URLItem> statusObserver2 =
                new StreamObserver<>() {

                    @Override
                    public void onNext(URLItem value) {
                        // receives confirmation that the value has been received
                        logURLItem(value);

                        // Internally, MemoryFrontierService does not make a distinction
                        // between discovered and known which have to be re-fetched
                        if (value.hasKnown()) {
                            assertEquals(refetchDate, value.getKnown().getRefetchableFromDate());
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

        rocksDBService.getURLStatus(request, statusObserver2);
    }
}
