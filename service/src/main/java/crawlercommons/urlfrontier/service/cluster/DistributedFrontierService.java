/**
 * SPDX-FileCopyrightText: 2022 Crawler-commons SPDX-License-Identifier: Apache-2.0 Licensed to
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
package crawlercommons.urlfrontier.service.cluster;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.Local;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams;
import crawlercommons.urlfrontier.Urlfrontier.Pagination;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;

public abstract class DistributedFrontierService extends AbstractFrontierService
        implements Closeable {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(DistributedFrontierService.class);

    protected boolean clusterMode = false;

    private final CacheLoader<String, ManagedChannel> loader =
            new CacheLoader<String, ManagedChannel>() {
                @Override
                public ManagedChannel load(String target) {
                    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                }
            };

    private final RemovalListener<String, ManagedChannel> listener =
            new RemovalListener<String, ManagedChannel>() {
                @Override
                public void onRemoval(RemovalNotification<String, ManagedChannel> n) {
                    n.getValue().shutdownNow();
                }
            };

    private LoadingCache<String, ManagedChannel> cache =
            CacheBuilder.newBuilder()
                    .removalListener(listener)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build(loader);

    private URLFrontierBlockingStub getFrontier(String target) {
        return URLFrontierGrpc.newBlockingStub(cache.getUnchecked(target));
    }

    /** Delete the queue based on the key in parameter */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long> responseObserver) {

        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());

        int sizeQueue = 0;

        if (!request.getLocal() || !clusterMode) {
            for (String node : getNodes()) {
                if (node.equals(address)) continue;
                // call the delete endpoint in the target node
                // force to local so that remote node don't go recursive
                QueueWithinCrawlParams local =
                        QueueWithinCrawlParams.newBuilder(request).setLocal(true).build();
                URLFrontierBlockingStub blockingFrontier = getFrontier(node);
                crawlercommons.urlfrontier.Urlfrontier.Long total =
                        blockingFrontier.deleteQueue(local);
                sizeQueue += total.getValue();
            }
        }
        // delete the queue held by this node
        sizeQueue += deleteLocalQueue(qc);

        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder()
                        .setValue(sizeQueue)
                        .build());
        responseObserver.onCompleted();
    }

    protected abstract int deleteLocalQueue(final QueueWithinCrawl qc);

    @Override
    public void deleteCrawl(
            crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage message,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
                    responseObserver) {

        if (!clusterMode) {
            super.deleteCrawl(message, responseObserver);
            return;
        }

        long total = 0;
        final String normalisedCrawlID = CrawlID.normaliseCrawlID(message.getValue());

        // distributed mode
        if (!message.getLocal()) {
            // force to local so that remote node don't go recursive
            DeleteCrawlMessage local =
                    DeleteCrawlMessage.newBuilder()
                            .setLocal(true)
                            .setValue(message.getValue())
                            .build();
            for (String node : getNodes()) {
                if (node.equals(address)) continue;
                // call the delete endpoint in the target node
                URLFrontierBlockingStub blockingFrontier = getFrontier(node);
                crawlercommons.urlfrontier.Urlfrontier.Long localCount =
                        blockingFrontier.deleteCrawl(local);
                total += localCount.getValue();
            }
        }

        // delete on the current node
        total += deleteLocalCrawl(normalisedCrawlID);

        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder().setValue(total).build());
        responseObserver.onCompleted();
    }

    protected abstract long deleteLocalCrawl(String crawlID);

    @Override
    public void getStats(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<Stats> responseObserver) {
        LOG.info("Received stats request");

        if (request.getLocal() || !clusterMode) {
            super.getStats(request, responseObserver);
            return;
        }

        final String normalisedCrawlID = CrawlID.normaliseCrawlID(request.getCrawlID());
        long numQueues = 0;
        long size = 0;
        int inProc = 0;
        Map<String, Long> counts = new HashMap<>();

        // force to local so that remote nodes don't go recursive
        QueueWithinCrawlParams local =
                QueueWithinCrawlParams.newBuilder(request).setLocal(true).build();
        for (String node : getNodes()) {
            URLFrontierBlockingStub blockingFrontier = getFrontier(node);
            Stats localStats = blockingFrontier.getStats(local);
            numQueues += localStats.getNumberOfQueues();
            size += localStats.getSize();
            inProc += localStats.getInProcess();
            for (Entry<String, Long> entry : localStats.getCountsMap().entrySet()) {
                counts.compute(
                        entry.getKey(),
                        (w, prev) ->
                                prev != null
                                        ? prev + entry.getValue().longValue()
                                        : entry.getValue().longValue());
            }
        }

        Stats stats =
                Stats.newBuilder()
                        .setNumberOfQueues(numQueues)
                        .setSize(size)
                        .setInProcess(inProc)
                        .putAllCounts(counts)
                        .setCrawlID(normalisedCrawlID)
                        .build();
        responseObserver.onNext(stats);
        responseObserver.onCompleted();
    }

    @Override
    public void setLogLevel(LogLevelParams request, StreamObserver<Empty> responseObserver) {
        if (!request.getLocal() || clusterMode) {
            // force to local so that remote node don't go recursive
            LogLevelParams local = LogLevelParams.newBuilder(request).setLocal(true).build();
            for (String node : getNodes()) {
                // exclude the local node
                if (node.equals(address)) continue;
                URLFrontierBlockingStub blockingFrontier = getFrontier(node);
                blockingFrontier.setLogLevel(local);
            }
        }
        super.setLogLevel(request, responseObserver);
    }

    @Override
    public void listCrawls(
            crawlercommons.urlfrontier.Urlfrontier.Local request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                    responseObserver) {

        Set<String> crawlIDs = new HashSet<>();

        if (!request.getLocal() || clusterMode) {
            // force to local so that remote node don't go recursive
            Local local = Local.newBuilder().setLocal(true).build();
            for (String node : getNodes()) {
                // exclude the local node
                if (node.equals(address)) continue;
                URLFrontierBlockingStub blockingFrontier = getFrontier(node);
                StringList results = blockingFrontier.listCrawls(local);
                for (String s : results.getValuesList()) {
                    crawlIDs.add(s);
                }
            }
        }

        synchronized (queues) {
            Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                    queues.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
                crawlIDs.add(e.getKey().getCrawlid());
            }
        }
        responseObserver.onNext(StringList.newBuilder().addAllValues(crawlIDs).build());
        responseObserver.onCompleted();
    }

    public void listQueues(
            crawlercommons.urlfrontier.Urlfrontier.Pagination request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.QueueList>
                    responseObserver) {

        if (request.getLocal() || !clusterMode) {
            super.listQueues(request, responseObserver);
            return;
        }

        Set<String> dedup = new HashSet<>();

        Pagination localPagination = Pagination.newBuilder(request).setLocal(true).build();
        for (String node : getNodes()) {
            URLFrontierBlockingStub blockingFrontier = getFrontier(node);
            QueueList listqueues = blockingFrontier.listQueues(localPagination);
            for (String s : listqueues.getValuesList()) {
                dedup.add(s);
            }
        }

        crawlercommons.urlfrontier.Urlfrontier.QueueList.Builder list = QueueList.newBuilder();
        list.addAllValues(dedup);
        responseObserver.onNext(list.build());
        responseObserver.onCompleted();
    }

    @Override
    public void close() throws IOException {
        // close all the connections
        cache.invalidateAll();
    }

    /** Sends the incoming items to a node based on the queue hash * */
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>
                    responseObserver) {

        putURLs_calls.inc();

        return new StreamObserver<URLItem>() {

            @Override
            public void onNext(URLItem value) {

                URLInfo info;

                if (value.hasDiscovered()) {
                    info = value.getDiscovered().getInfo();
                } else {
                    KnownURLItem known = value.getKnown();
                    info = known.getInfo();
                }

                String Qkey = info.getKey();
                String url = info.getUrl();
                String crawlID = CrawlID.normaliseCrawlID(info.getCrawlID());

                // has a queue key been defined? if not use the hostname
                if (Qkey.equals("")) {
                    LOG.debug("key missing for {}", url);
                    Qkey = provideMissingKey(url);
                    if (Qkey == null) {
                        LOG.error("Malformed URL {}", url);
                        responseObserver.onNext(
                                crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                        .setValue(url)
                                        .build());
                        return;
                    }
                    // make a new info object ready to return
                    info = URLInfo.newBuilder(info).setKey(Qkey).setCrawlID(crawlID).build();
                }

                QueueWithinCrawl qk = QueueWithinCrawl.get(Qkey, crawlID);

                // work out which node should receive the item
                int partition = Math.abs(qk.toString().hashCode() % getNodes().size());

                // is it the local node?
                int index = getNodes().indexOf(address);
                if (index == -1) {
                    throw new RuntimeException(
                            "ShardedRocksDBService found conf 'nodes' but current node's address not set");
                }

                if (partition == index) {
                    putURLItem(value);
                    responseObserver.onNext(
                            crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                    .setValue(url)
                                    .build());
                } else {
                    // forward to non-local node
                    // should not happen very frequently unless a crawler
                    // allows outlinks to go outside the hostname

                    final AtomicBoolean completed = new AtomicBoolean(false);

                    final URLFrontierStub stub =
                            URLFrontierGrpc.newStub(cache.getUnchecked(getNodes().get(index)));

                    final StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> observer =
                            new StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>() {

                                @Override
                                public void onNext(
                                        crawlercommons.urlfrontier.Urlfrontier.String value) {
                                    // forwards confirmation that the value has been received
                                    responseObserver.onNext(value);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    completed.set(true);
                                    LOG.error("Caught throwable when forwardng request ", t);
                                }

                                @Override
                                public void onCompleted() {
                                    completed.set(true);
                                }
                            };

                    final StreamObserver<URLItem> streamObserver = stub.putURLs(observer);

                    streamObserver.onNext(value);

                    streamObserver.onCompleted();

                    // wait for completion
                    while (!completed.get()) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                LOG.error("Throwable caught", t);
            }

            @Override
            public void onCompleted() {
                // will this ever get called if the client is constantly streaming?
                responseObserver.onCompleted();
            }
        };
    }

    protected abstract String putURLItem(URLItem item);
}
