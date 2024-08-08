// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceTestUtil {

    public static void initURLs(AbstractFrontierService service) {

        String crawlId = "crawl_id";
        String url1 = "https://www.mysite.com/discovered";
        String key1 = "queue_mysite";
        String url2 = "https://www.mysite.com/completed";
        String key2 = "queue_mysite";
        String url3 = "https://www.mysite.com/knowntorefetch";
        String key3 = "queue_mysite";

        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);
        final AtomicInteger skipped = new AtomicInteger(0);
        final AtomicInteger ok = new AtomicInteger(0);
        int sent = 0;

        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.AckMessage value) {
                        // receives confirmation that the value has been received
                        acked.addAndGet(1);
                        if (value.getStatus().equals(AckMessage.Status.SKIPPED)) {
                            skipped.getAndIncrement();
                        } else if (value.getStatus().equals(AckMessage.Status.FAIL)) {
                            failed.getAndIncrement();
                        } else if (value.getStatus().equals(AckMessage.Status.OK)) {
                            ok.getAndIncrement();
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
                    }
                };

        StreamObserver<URLItem> streamObserver = service.putURLs(responseObserver);

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder1 = URLItem.newBuilder();
        StringList sl1 = StringList.newBuilder().addValues("md1").build();
        StringList sl2 = StringList.newBuilder().addValues("md2").build();
        StringList sl3 = StringList.newBuilder().addValues("md3").build();

        URLInfo info1 =
                URLInfo.newBuilder()
                        .setUrl(url1)
                        .setCrawlID(crawlId)
                        .setKey(key1)
                        .putMetadata("meta1", sl1)
                        .build();

        DiscoveredURLItem disco1 = DiscoveredURLItem.newBuilder().setInfo(info1).build();
        builder1.setDiscovered(disco1);
        builder1.setID(crawlId + "_" + url1);

        streamObserver.onNext(builder1.build());
        sent++;

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

        streamObserver.onNext(builder1.build());
        sent++;

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder2 = URLItem.newBuilder();
        KnownURLItem known =
                KnownURLItem.newBuilder().setInfo(info2).setRefetchableFromDate(0).build();

        builder2.setKnown(known);
        builder2.setID(crawlId + "_" + url2);

        streamObserver.onNext(builder2.build());
        sent++;

        URLInfo info3 =
                URLInfo.newBuilder()
                        .setUrl(url3)
                        .setCrawlID(crawlId)
                        .setKey(key3)
                        .putMetadata("meta3", sl3)
                        .build();

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder3 = URLItem.newBuilder();
        KnownURLItem torefetch =
                KnownURLItem.newBuilder()
                        .setInfo(info3)
                        .setRefetchableFromDate(Instant.now().getEpochSecond() + 3600)
                        .build();

        builder3.setKnown(torefetch);
        builder3.setID(crawlId + "_" + url3);

        streamObserver.onNext(builder3.build());
        sent++;

        streamObserver.onCompleted();

        // wait for completion
        while (!completed.get() || sent != acked.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
