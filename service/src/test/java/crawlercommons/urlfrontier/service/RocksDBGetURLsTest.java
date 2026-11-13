// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.rocksdb.RocksDBService;
import io.grpc.stub.StreamObserver;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * getURLs on RocksDB must serve the URLs of a queue whose crawl ID or key contains an underscore,
 * which is escaped in the keys of the scheduling column family.
 */
class RocksDBGetURLsTest {

    static final String CRAWL_ID = "crawl_id";
    static final String KEY = "queue_mysite";
    static final String URL = "https://www.mysite.com/discovered";

    @TempDir Path rocksdbPath;

    @Test
    void servesQueueWithUnderscores() throws Exception {
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", rocksdbPath.toString());
        RocksDBService service = new RocksDBService(conf, "localhost", 7071);
        try {
            putDiscovered(service);

            List<URLInfo> received = new CopyOnWriteArrayList<>();
            CountDownLatch completed = new CountDownLatch(1);
            StreamObserver<URLInfo> observer =
                    new StreamObserver<>() {
                        @Override
                        public void onNext(URLInfo value) {
                            received.add(value);
                        }

                        @Override
                        public void onError(Throwable t) {
                            completed.countDown();
                        }

                        @Override
                        public void onCompleted() {
                            completed.countDown();
                        }
                    };

            GetParams request =
                    GetParams.newBuilder()
                            .setKey(KEY)
                            .setCrawlID(CRAWL_ID)
                            .setMaxUrlsPerQueue(10)
                            .setDelayRequestable(30)
                            .build();
            service.getURLs(request, observer);
            assertTrue(completed.await(10, TimeUnit.SECONDS));

            assertEquals(1, received.size());
            assertEquals(URL, received.get(0).getUrl());
            assertEquals(CRAWL_ID, received.get(0).getCrawlID());
            assertEquals(KEY, received.get(0).getKey());
        } finally {
            service.close();
        }
    }

    private static void putDiscovered(RocksDBService service) throws Exception {
        AtomicBoolean done = new AtomicBoolean(false);
        AtomicInteger acked = new AtomicInteger();
        StreamObserver<URLItem> put =
                service.putURLs(
                        new StreamObserver<>() {
                            @Override
                            public void onNext(AckMessage value) {
                                acked.incrementAndGet();
                            }

                            @Override
                            public void onError(Throwable t) {
                                done.set(true);
                            }

                            @Override
                            public void onCompleted() {
                                done.set(true);
                            }
                        });
        URLInfo info = URLInfo.newBuilder().setUrl(URL).setCrawlID(CRAWL_ID).setKey(KEY).build();
        put.onNext(
                URLItem.newBuilder()
                        .setID(CRAWL_ID + "_" + URL)
                        .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                        .build());
        put.onCompleted();
        ServiceTestUtil.awaitStreamClosed(done);
        assertEquals(1, acked.get());
    }
}
