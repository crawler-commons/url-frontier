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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
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
import crawlercommons.urlfrontier.service.SynchronizedStreamObserver;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.LoggerFactory;

public abstract class DistributedFrontierService extends AbstractFrontierService {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(DistributedFrontierService.class);

    protected boolean clusterMode = false;

    private final CacheLoader<String, ManagedChannel> channelLoader =
            new CacheLoader<String, ManagedChannel>() {
                @Override
                public ManagedChannel load(String target) {
                    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                }
            };

    private final RemovalListener<String, ManagedChannel> channelRemovalListener =
            new RemovalListener<String, ManagedChannel>() {
                @Override
                public void onRemoval(RemovalNotification<String, ManagedChannel> n) {
                    n.getValue().shutdownNow();
                }
            };

    private LoadingCache<String, ManagedChannel> channelCache =
            CacheBuilder.newBuilder().removalListener(channelRemovalListener).build(channelLoader);

    private URLFrontierBlockingStub getFrontier(String target) {
        return URLFrontierGrpc.newBlockingStub(channelCache.getUnchecked(target));
    }

    private final CacheLoader<Integer, StreamObserver<URLItem>> observerloader =
            new CacheLoader<>() {
                @Override
                public StreamObserver<URLItem> load(Integer index) {
                    // get the stream observer for that node
                    final StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                            observer;

                    final String nodeAddress = getNodes().get(index);

                    final URLFrontierStub stub =
                            URLFrontierGrpc.newStub(channelCache.getUnchecked(nodeAddress));

                    observer =
                            new StreamObserver<>() {

                                @Override
                                public void onNext(
                                        crawlercommons.urlfrontier.Urlfrontier.AckMessage value) {

                                    // go back to the client
                                    // and notify that it has worked
                                    // we know that the observer in the cache
                                    // is a synchronized one
                                    StreamObserver<AckMessage> stream =
                                            inprocesscache.getIfPresent(value.getID());
                                    if (stream != null) {
                                        LOG.debug(
                                                "Got stream to ack back for {} with status {}",
                                                value.getID(),
                                                value.getStatus());
                                        try {
                                            stream.onNext(value);
                                        } catch (Exception e) {
                                            LOG.error(
                                                    "Error while communicating back with the client: {} ",
                                                    e.getLocalizedMessage());
                                        }
                                    } else {
                                        LOG.error(
                                                "No stream found to ack back for {} with status {}",
                                                value.getID(),
                                                value.getStatus());
                                    }
                                    // remove it whether we have been able to return the value or
                                    // not
                                    inprocesscache.invalidate(value.getID());
                                }

                                @Override
                                public void onError(Throwable t) {
                                    observercache.invalidate(index);
                                    if (t instanceof StatusRuntimeException) {
                                        // ignore messages about the client having cancelled
                                        if (((StatusRuntimeException) t)
                                                .getStatus()
                                                .getCode()
                                                .equals(io.grpc.Status.Code.CANCELLED)) {
                                            return;
                                        }
                                    }
                                    LOG.error(
                                            "Caught throwable when forwarding request to shard {}: {}",
                                            index,
                                            t.getLocalizedMessage());
                                }

                                @Override
                                public void onCompleted() {
                                    // finished?
                                    observercache.invalidate(index);
                                }
                            };

                    return stub.putURLs(observer);
                }
            };

    private final RemovalListener<String, StreamObserver<URLItem>> observerlistener =
            new RemovalListener<>() {
                @Override
                public void onRemoval(RemovalNotification<String, StreamObserver<URLItem>> n) {
                    LOG.info("Removed StreamObserver {} with key {}", n.getValue(), n.getKey());
                }
            };

    private LoadingCache<Integer, StreamObserver<URLItem>> observercache =
            CacheBuilder.newBuilder()
                    .removalListener(observerlistener)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build((CacheLoader) observerloader);

    private final RemovalListener<
                    String, StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>>
            inProcessRemovalListener =
                    new RemovalListener<>() {
                        @Override
                        public void onRemoval(
                                RemovalNotification<String, StreamObserver<AckMessage>>
                                        notification) {
                            // try to notify the client if something was sent to another instance
                            // but never
                            // came back?
                            if (notification.wasEvicted()) {
                                String ID = notification.getKey();
                                LOG.debug(
                                        "Trying to notify original stream about eviction of {}",
                                        ID);
                                StreamObserver<AckMessage> stream = notification.getValue();
                                if (stream != null) {
                                    try {
                                        stream.onNext(
                                                AckMessage.newBuilder()
                                                        .setID(ID)
                                                        .setStatus(Status.FAIL)
                                                        .build());
                                    } catch (Exception e) {
                                        LOG.error(
                                                "Error while communicating back with the client: {} ",
                                                e.getLocalizedMessage());
                                    }
                                }
                            }
                        }
                    };

    private Cache<String, StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>>
            inprocesscache =
                    CacheBuilder.newBuilder()
                            .expireAfterAccess(1, TimeUnit.MINUTES)
                            .removalListener(inProcessRemovalListener)
                            .build();

    /** Delete the queue based on the key in parameter */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long> responseObserver) {

        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());

        int sizeQueue = 0;

        if (!request.getLocal() && clusterMode) {
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
        if (!request.getLocal() && clusterMode) {
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

        if (!request.getLocal() && clusterMode) {
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

        synchronized (getQueues()) {
            Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                    getQueues().entrySet().iterator();
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
        super.close();
        // close all the connections
        channelCache.invalidateAll();
    }

    /** Sends the incoming items to a node based on the queue hash * */
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.URLItem> putURLs(
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                    responseObserver) {

        putURLs_calls.inc();

        ((ServerCallStreamObserver<AckMessage>) responseObserver)
                .setOnCancelHandler(
                        () -> {
                            LOG.error("Client cancelled");
                        });

        // wrap the response observer as a synchronized one
        StreamObserver<AckMessage> sso = SynchronizedStreamObserver.wrapping(responseObserver);

        return new StreamObserver<URLItem>() {

            final AtomicInteger unacked = new AtomicInteger();

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

                crawlercommons.urlfrontier.Urlfrontier.AckMessage.Builder ack =
                        AckMessage.newBuilder();
                if (value.getID() == null || value.getID().isEmpty()) {
                    ack.setID(url);
                } else {
                    ack.setID(value.getID());
                }

                // has a queue key been defined? if not use the hostname
                if (Qkey.equals("")) {
                    LOG.debug("key missing for {}", url);
                    Qkey = provideMissingKey(url);
                    if (Qkey == null) {
                        LOG.error("Malformed URL {}", url);
                        sso.onNext(ack.setStatus(Status.SKIPPED).build());
                        return;
                    }
                    // make a new info object ready to return
                    info = URLInfo.newBuilder(info).setKey(Qkey).setCrawlID(crawlID).build();
                }

                LOG.debug("Received {} with queue {} and crawlid {}", url, Qkey, crawlID);

                final QueueWithinCrawl qk = QueueWithinCrawl.get(Qkey, crawlID);

                // work out which node should receive the item
                int partition = Math.abs(qk.toString().hashCode() % getNodes().size());

                // is it the local node?
                int localNodeIndex = getNodes().indexOf(address);
                if (localNodeIndex == -1) {
                    throw new RuntimeException(
                            "ShardedRocksDBService found conf 'nodes' but current node's address not set");
                }

                LOG.trace("LocalNodeIndex {}", localNodeIndex);

                if (partition == localNodeIndex) {
                    unacked.incrementAndGet();
                    writeExecutorService.execute(
                            () -> {
                                Status s = putURLItem(value);
                                LOG.debug("Local putURL -> {} got status {}", url, s);
                                sso.onNext(ack.setStatus(s).build());
                                unacked.decrementAndGet();
                            });
                    return;
                }

                // forward to non-local node

                // get the stream observer for that node
                final StreamObserver<URLItem> observer = observercache.getUnchecked(partition);
                // store the stuff in a temporary cache
                inprocesscache.put(ack.getID(), sso);

                LOG.debug(
                        "Sending {} to partition {} -> {}",
                        url,
                        partition,
                        getNodes().get(partition));

                // give it the thing to process
                observer.onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                if (t instanceof StatusRuntimeException) {
                    // ignore messages about the client having cancelled
                    if (((StatusRuntimeException) t)
                            .getStatus()
                            .getCode()
                            .equals(io.grpc.Status.Code.CANCELLED)) {
                        return;
                    }
                }
                LOG.error("Throwable caught", t.getLocalizedMessage());
            }

            @Override
            public void onCompleted() {
                // check that all the work for this stream has ended
                while (unacked.get() != 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                sso.onCompleted();
            }
        };
    }

    protected abstract Status putURLItem(URLItem item);
}
