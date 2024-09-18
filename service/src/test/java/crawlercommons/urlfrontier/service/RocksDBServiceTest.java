// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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
}
