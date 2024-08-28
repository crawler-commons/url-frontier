package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.rocksdb.RocksDBService;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class RocksDBServiceTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RocksDBServiceTest.class);

    RocksDBService rocksDBService;

    @AfterEach
    void shutdown() throws IOException {
        rocksDBService.close();
    }

    @BeforeEach
    void setup() {

        rocksDBService = new RocksDBService("localhost", 7071);
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
}
