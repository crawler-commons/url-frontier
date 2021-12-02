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

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
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

        LOG.info("Initialisation of connection to URLFrontier service on {}:{}", host, port);

        channel =
                ManagedChannelBuilder.forAddress(host, Integer.parseInt(port))
                        .usePlaintext()
                        .build();
        frontier = URLFrontierGrpc.newStub(channel);
        blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
    }

    @After
    public void shutdown() {
        LOG.info("Shutting down connection to URLFrontier service");
        channel.shutdown();
    }

    @Test
    public void testUpdates() {

        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver =
                new StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.String value) {
                        // receives confirmation that the value has been received
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

        // inject 2 duplicate discovered URLs
        URLInfo info = URLInfo.newBuilder().setUrl("http://key1.com/").setKey("key1.com").build();

        DiscoveredURLItem item = DiscoveredURLItem.newBuilder().setInfo(info).build();
        DiscoveredURLItem item2 = DiscoveredURLItem.newBuilder().setInfo(info).build();

        streamObserver.onNext(URLItem.newBuilder().setDiscovered(item).build());
        streamObserver.onNext(URLItem.newBuilder().setDiscovered(item2).build());

        streamObserver.onCompleted();

        LOG.info("Sending URL: {}", item);

        // wait for completion
        while (completed.get() == false) {
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException e) {
            }
        }

        Assert.assertEquals("incorrect number of url acked", 2, acked.get());

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

        // want just one URL for that specific key
        GetParams request2 =
                GetParams.newBuilder()
                        .setKey("key1.com")
                        .setMaxUrlsPerQueue(1)
                        .setDelayRequestable(delayrequestable)
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

        crawlercommons.urlfrontier.Urlfrontier.Integer deleted =
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
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver =
                new StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>() {

                    @Override
                    public void onNext(crawlercommons.urlfrontier.Urlfrontier.String value) {
                        // receives confirmation that the value has been received
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

        URLInfo info = URLInfo.newBuilder().setUrl("http://gac.icann.org?language_id=9").build();

        DiscoveredURLItem item = DiscoveredURLItem.newBuilder().setInfo(info).build();

        streamObserver.onNext(URLItem.newBuilder().setDiscovered(item).build());

        streamObserver.onCompleted();

        LOG.info("Sending URL: {}", item);

        // wait for completion
        while (completed.get() == false) {
            try {
                Thread.currentThread().sleep(10);
            } catch (InterruptedException e) {
            }
        }

        Stats stats =
                blockingFrontier.getStats(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder()
                                .setKey("gac.icann.org")
                                .build());
        Assert.assertEquals("number of queues matching", 1, stats.getSize());

        crawlercommons.urlfrontier.Urlfrontier.Integer deleted =
                blockingFrontier.deleteQueue(
                        Urlfrontier.QueueWithinCrawlParams.newBuilder()
                                .setKey("gac.icann.org")
                                .build());

        Assert.assertEquals("incorrect number of queues deleted", 1, deleted.getValue());
    }

    @Test
    public void testCrawlIDs() {
        StringList crawlids = blockingFrontier.listCrawls(Urlfrontier.Empty.getDefaultInstance());

        Assert.assertEquals("incorrect number of crawlids", 1, crawlids.getValuesCount());
    }
}
