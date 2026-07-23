// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** setCrawlLimit is a unary RPC returning Empty: exactly one response must be emitted (#153). */
class SetCrawlLimitTest {

    static final String CRAWL_ID = "crawl_id";
    static final String KEY = "queue_mysite";

    static MemoryFrontierService serviceWithOneQueue() throws Exception {
        MemoryFrontierService service = new MemoryFrontierService("localhost", 0);
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
        URLInfo info =
                URLInfo.newBuilder()
                        .setUrl("https://www.mysite.com/a")
                        .setCrawlID(CRAWL_ID)
                        .setKey(KEY)
                        .build();
        put.onNext(
                URLItem.newBuilder()
                        .setID(CRAWL_ID + "_a")
                        .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                        .build());
        put.onCompleted();
        while (!done.get() || acked.get() != 1) {
            Thread.sleep(5);
        }
        return service;
    }

    static class EmptyObserver implements StreamObserver<Empty> {
        final AtomicInteger responses = new AtomicInteger();
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void onNext(Empty value) {
            responses.incrementAndGet();
        }

        @Override
        public void onError(Throwable t) {
            error.set(t);
            done.countDown();
        }

        @Override
        public void onCompleted() {
            done.countDown();
        }
    }

    @Test
    void setCrawlLimitEmitsTheEmptyResponse() throws Exception {
        MemoryFrontierService service = serviceWithOneQueue();

        EmptyObserver observer = new EmptyObserver();
        service.setCrawlLimit(
                CrawlLimitParams.newBuilder().setKey(KEY).setCrawlID(CRAWL_ID).setLimit(5).build(),
                observer);

        assertTrue(observer.done.await(5, TimeUnit.SECONDS));
        assertNull(observer.error.get());
        assertEquals(
                1, observer.responses.get(), "unary call must emit exactly one Empty response");
    }

    @Test
    void setCrawlLimitOnUnknownQueueErrors() throws Exception {
        MemoryFrontierService service = serviceWithOneQueue();

        EmptyObserver observer = new EmptyObserver();
        service.setCrawlLimit(
                CrawlLimitParams.newBuilder()
                        .setKey("nosuchqueue")
                        .setCrawlID(CRAWL_ID)
                        .setLimit(5)
                        .build(),
                observer);

        assertTrue(observer.done.await(5, TimeUnit.SECONDS));
        assertNotNull(observer.error.get());
        assertEquals(0, observer.responses.get());
    }
}
