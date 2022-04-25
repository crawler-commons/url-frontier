/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons SPDX-License-Identifier: Apache-2.0 Licensed to
 * Crawler-Commons under one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership. DigitalPebble licenses
 * this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class URLFrontierServiceTest {

    private ManagedChannel channel;

    private URLFrontierStub frontier;

    private URLFrontierBlockingStub blockingFrontier;

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(URLFrontierServiceTest.class);

    @Before
    public void init() throws IOException {

        String host = System.getProperty("urlfrontier.host");
        String port = System.getProperty("urlfrontier.port");

        // use default values
        if (host == null) {
            host = "localhost";
        }

        if (port == null) {
            port = "7071";
        }

        LOG.info("Initialisation of connection to URLFrontier service on {}:{}", host, port);

        channel =
                ManagedChannelBuilder.forAddress(host, Integer.parseInt(port))
                        .usePlaintext()
                        .build();
        frontier = URLFrontierGrpc.newStub(channel);
        blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        // delete anything that could have been left from a previous run
        blockingFrontier.deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage.newBuilder()
                        .setValue("BESPOKE")
                        .build());

        blockingFrontier.deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage.newBuilder().build());
    }

    @After
    public void shutdown() {
        LOG.info("Shutting down connection to URLFrontier service");
        channel.shutdown();
    }

    @Test
    public void NeverRefetched() {

        // inject 2 duplicate discovered URLs
        URLInfo info = URLInfo.newBuilder().setUrl("http://key1.com/").setKey("key1.com").build();

        KnownURLItem item = KnownURLItem.newBuilder().setInfo(info).build();
        int acked =
                sendURLs(
                        new URLItem[] {
                            URLItem.newBuilder().setKnown(item).build(),
                        });

        Assert.assertEquals("incorrect number of url acked", 1, acked);

        /** Get the URLs due for fetching for a specific key * */
        int delayrequestable = 2;

        // want just one URL for that specific key, in the default crawl
        GetParams request2 =
                GetParams.newBuilder()
                        .setKey("key1.com")
                        .setMaxUrlsPerQueue(1)
                        .setDelayRequestable(delayrequestable)
                        .setCrawlID(CrawlID.DEFAULT)
                        .build();

        boolean hasAny = blockingFrontier.getURLs(request2).hasNext();

        Assert.assertEquals("Should not return URLs", false, hasAny);
    }

    @Test
    public void RightOrder() {

        URLInfo info =
                URLInfo.newBuilder().setUrl("http://key1.com/later").setKey("key1.com").build();
        URLInfo info2 =
                URLInfo.newBuilder().setUrl("http://key1.com/sooner").setKey("key1.com").build();

        KnownURLItem item =
                KnownURLItem.newBuilder()
                        .setInfo(info)
                        .setRefetchableFromDate(Instant.now().getEpochSecond())
                        .build();
        KnownURLItem item2 =
                KnownURLItem.newBuilder()
                        .setInfo(info2)
                        .setRefetchableFromDate(Instant.now().getEpochSecond() - 10000)
                        .build();

        int acked =
                sendURLs(
                        new URLItem[] {
                            URLItem.newBuilder().setKnown(item).build(),
                            URLItem.newBuilder().setKnown(item2).build()
                        });

        Assert.assertEquals("incorrect number of url acked", 2, acked);

        /** Get the URLs due for fetching for a specific key * */
        int delayrequestable = 2;

        // want just one URL for that specific key, in the default crawl
        GetParams request2 =
                GetParams.newBuilder()
                        .setKey("key1.com")
                        .setMaxUrlsPerQueue(2)
                        .setDelayRequestable(delayrequestable)
                        .setCrawlID(CrawlID.DEFAULT)
                        .build();

        // 2nd doc should be returned before the first

        String URLFirst = blockingFrontier.getURLs(request2).next().getUrl();

        Assert.assertEquals("Should not return URLs", "http://key1.com/sooner", URLFirst);
    }

    @Test
    public void testUpdates() {

        // inject 2 duplicate discovered URLs
        URLInfo info = URLInfo.newBuilder().setUrl("http://key1.com/").setKey("key1.com").build();

        DiscoveredURLItem item = DiscoveredURLItem.newBuilder().setInfo(info).build();
        DiscoveredURLItem item2 = DiscoveredURLItem.newBuilder().setInfo(info).build();

        int acked =
                sendURLs(
                        new URLItem[] {
                            URLItem.newBuilder().setDiscovered(item).build(),
                            URLItem.newBuilder().setDiscovered(item2).build()
                        });

        Assert.assertEquals("incorrect number of url acked", 2, acked);

        /** The methods below use the blocking API * */

        // check that we have one queue for it

        LOG.info("Checking existence of queue");

        crawlercommons.urlfrontier.Urlfrontier.Pagination request =
                crawlercommons.urlfrontier.Urlfrontier.Pagination.newBuilder().build();
        QueueList queueslisted = blockingFrontier.listQueues(request);

        Assert.assertEquals(
                "incorrect number of queues returned", 1, queueslisted.getValuesCount());

        LOG.info("Received {} queue - 1 expected", queueslisted.getValuesCount());

        /** Get the URLs due for fetching for a specific key * */
        int delayrequestable = 2;

        // want just one URL for that specific key, in the default crawl
        GetParams request2 =
                GetParams.newBuilder()
                        .setKey("key1.com")
                        .setMaxUrlsPerQueue(1)
                        .setDelayRequestable(delayrequestable)
                        .setCrawlID(CrawlID.DEFAULT)
                        .build();

        String urlreturned = blockingFrontier.getURLs(request2).next().getUrl();

        Assert.assertEquals("incorrect number of URLs returned", "http://key1.com/", urlreturned);

        /** Get stats about the queue * */
        Stats stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder().setKey("key1.com").build());

        Assert.assertEquals("incorrect number of queues from stats", 1, stats.getNumberOfQueues());

        // should still have one URL marked as in process
        Assert.assertEquals("incorrect number of inprocesss from stats", 1, stats.getInProcess());

        // try getting an URL for that key again - should get blocked because it is in
        // process
        boolean hasMore = blockingFrontier.getURLs(request2).hasNext();

        Assert.assertEquals("should not return a URL", false, hasMore);

        // wait delay requestable
        try {
            Thread.sleep(delayrequestable * 1000);
        } catch (InterruptedException e) {
        }

        // bug fix in 0.2
        stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder().setKey("key1.com").build());

        Assert.assertEquals("incorrect number of queues from stats", 1, stats.getNumberOfQueues());
        Assert.assertEquals("incorrect number of in process from stats", 0, stats.getInProcess());

        crawlercommons.urlfrontier.Urlfrontier.Long deleted =
                blockingFrontier.deleteQueue(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder().setKey("key1.com").build());

        Assert.assertEquals("incorrect number of queues deleted", 1, deleted.getValue());

        stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder().setKey("key1.com").build());

        Assert.assertEquals("incorrect number of queues from stats", 0, stats.getNumberOfQueues());
    }

    @Test
    public void testQueueNames() {

        URLInfo info = URLInfo.newBuilder().setUrl("http://gac.icann.org?language_id=9").build();

        DiscoveredURLItem disco = DiscoveredURLItem.newBuilder().setInfo(info).build();

        URLItem item = URLItem.newBuilder().setDiscovered(disco).build();

        sendURLs(item);

        Stats stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder()
                                .setKey("gac.icann.org")
                                .build());
        Assert.assertEquals("number of queues matching", 1, stats.getSize());

        crawlercommons.urlfrontier.Urlfrontier.Long deleted =
                blockingFrontier.deleteQueue(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder()
                                .setKey("gac.icann.org")
                                .build());

        Assert.assertEquals("incorrect number of queues deleted", 1, deleted.getValue());
    }

    @Test
    public void testCrawlIDs() {
        StringList crawlids = blockingFrontier.listCrawls(Urlfrontier.Local.getDefaultInstance());

        Assert.assertEquals("incorrect number of crawlids", 0, crawlids.getValuesCount());

        // custom crawl ID
        URLInfo info =
                URLInfo.newBuilder()
                        .setUrl("http://urlfrontier.net/")
                        .setCrawlID("BESPOKE")
                        .build();
        DiscoveredURLItem disco = DiscoveredURLItem.newBuilder().setInfo(info).build();

        sendURLs(URLItem.newBuilder().setDiscovered(disco).build());

        // implicit default crawl ID
        URLInfo info2 = URLInfo.newBuilder().setUrl("http://urlfrontier.net/").build();
        DiscoveredURLItem disco2 = DiscoveredURLItem.newBuilder().setInfo(info2).build();

        sendURLs(URLItem.newBuilder().setDiscovered(disco2).build());

        // explicit default crawl ID
        URLInfo info3 =
                URLInfo.newBuilder()
                        .setUrl("http://urlfrontier.net/")
                        .setCrawlID(CrawlID.DEFAULT)
                        .build();
        DiscoveredURLItem disco3 = DiscoveredURLItem.newBuilder().setInfo(info3).build();

        sendURLs(URLItem.newBuilder().setDiscovered(disco3).build());

        // now check the number of URLs in default crawl
        Stats stats =
                blockingFrontier.getStats(Urlfrontier.QueueWithinCrawlParams.newBuilder().build());

        Assert.assertEquals("incorrect number of URLs from stats", 1, stats.getSize());
        Assert.assertEquals("incorrect crawlID", CrawlID.DEFAULT, stats.getCrawlID());
        Assert.assertEquals("incorrect number of queues from stats", 1, stats.getNumberOfQueues());

        // now check the number of URLs in custom crawl
        stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder()
                                .setCrawlID("BESPOKE")
                                .build());

        Assert.assertEquals("incorrect number of URLs from stats", 1, stats.getSize());
        Assert.assertEquals("incorrect crawlID", "BESPOKE", stats.getCrawlID());
        Assert.assertEquals("incorrect number of queues from stats", 1, stats.getNumberOfQueues());

        blockingFrontier.deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage.newBuilder()
                        .setValue("BESPOKE")
                        .build());

        stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder()
                                .setCrawlID("BESPOKE")
                                .build());

        Assert.assertEquals("incorrect crawlID", "BESPOKE", stats.getCrawlID());
        Assert.assertEquals("incorrect number of URLs from stats", 0, stats.getSize());

        blockingFrontier.deleteCrawl(
                crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage.newBuilder().build());

        stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder().setCrawlID("").build());

        Assert.assertEquals("incorrect number of URLs from stats", 0, stats.getSize());
    }

    private final int sendURLs(URLItem... items) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver =
                new StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.String value) {
                        acked.addAndGet(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        completed.set(true);
                        LOG.info("Error received", t);
                    }

                    @Override
                    public void onCompleted() {
                        completed.set(true);
                    }
                };

        StreamObserver<URLItem> streamObserver = frontier.putURLs(responseObserver);

        for (URLItem item : items) {
            streamObserver.onNext(item);
            LOG.info("Sending URL: {}", item);
        }

        streamObserver.onCompleted();

        // wait for completion
        while (completed.get() == false) {
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException e) {
            }
        }

        return acked.get();
    }
}
