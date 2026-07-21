// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

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
import crawlercommons.urlfrontier.Urlfrontier.Active;
import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.Local;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams;
import crawlercommons.urlfrontier.Urlfrontier.Pagination;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.LoggerFactory;

public abstract class DistributedFrontierService extends AbstractFrontierService {

    public DistributedFrontierService(
            final Map<String, String> configuration, String host, int port) {
        super(configuration, host, port);
    }

    // no explicit config
    public DistributedFrontierService(String host, int port) {
        this(new HashMap<String, String>(), host, port);
    }

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(DistributedFrontierService.class);

    protected boolean clusterMode = false;

    /** Maximum time granted to a forwarded control call before failing the caller. */
    static final int FORWARD_DEADLINE_SECONDS = 30;

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

    /**
     * Identifies the partition (index in the sorted node list) owning a queue. Must stay
     * bit-for-bit identical to the historical inline computation in putURLs: changing it (e.g. to
     * floorMod) would silently remap existing queues without migration.
     */
    static int partitionFor(QueueWithinCrawl qwc, List<String> nodes) {
        return Math.abs(qwc.toString().hashCode() % nodes.size());
    }

    @FunctionalInterface
    interface LocalEmptyCall {
        void invoke(StreamObserver<Empty> observer);
    }

    @FunctionalInterface
    interface RemoteEmptyCall {
        void invoke(URLFrontierStub stub, StreamObserver<Empty> observer);
    }

    private URLFrontierStub getAsyncFrontier(String target) {
        return URLFrontierGrpc.newStub(channelCache.getUnchecked(target))
                .withDeadlineAfter(FORWARD_DEADLINE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Runs the call on the node owning the queue: locally when this node is the owner, otherwise
     * forwarded once to the owner with a deadline. Exactly one terminal event reaches the response
     * observer.
     */
    private void routeToOwner(
            QueueWithinCrawl qwc,
            LocalEmptyCall localCall,
            RemoteEmptyCall remoteCall,
            StreamObserver<Empty> responseObserver) {
        final List<String> nodes = List.copyOf(getNodes());
        final int localNodeIndex = nodes.indexOf(address);
        if (localNodeIndex == -1) {
            throw new RuntimeException(
                    "Found conf 'nodes' but current node's address not in the list");
        }
        final int partition = partitionFor(qwc, nodes);
        if (partition == localNodeIndex) {
            localCall.invoke(responseObserver);
        } else {
            final EmptyAggregator aggregator = new EmptyAggregator(1, responseObserver);
            remoteCall.invoke(getAsyncFrontier(nodes.get(partition)), aggregator.newChild());
        }
    }

    /**
     * Applies the call locally first, then forwards it to every other node with a deadline, so a
     * fast remote failure can never unblock the caller before the local application has completed.
     * Success is reported only when all nodes responded; the first failure is reported instead. The
     * operation is not atomic: some nodes may have applied it when an error is returned. Repeating
     * the same assignment is idempotent in the absence of concurrent newer writes.
     */
    private void broadcastAll(
            LocalEmptyCall localCall,
            RemoteEmptyCall remoteCall,
            StreamObserver<Empty> responseObserver) {
        final List<String> nodes = List.copyOf(getNodes());
        if (nodes.indexOf(address) == -1) {
            throw new RuntimeException(
                    "Found conf 'nodes' but current node's address not in the list");
        }
        final EmptyAggregator aggregator = new EmptyAggregator(nodes.size(), responseObserver);
        localCall.invoke(aggregator.newChild());
        for (String node : nodes) {
            if (node.equals(address)) {
                continue;
            }
            remoteCall.invoke(getAsyncFrontier(node), aggregator.newChild());
        }
    }

    /**
     * In cluster mode, a keyed request with local=false is routed to the node owning the queue; a
     * keyless one (default delay) is applied on every node. local=true always stays local.
     */
    @Override
    public void setDelay(QueueDelayParams request, StreamObserver<Empty> responseObserver) {
        if (request.getLocal() || !clusterMode || isClosing()) {
            super.setDelay(request, responseObserver);
            return;
        }
        final QueueDelayParams localParams =
                QueueDelayParams.newBuilder(request).setLocal(true).build();
        if (request.getKey().isEmpty()) {
            broadcastAll(
                    observer -> super.setDelay(localParams, observer),
                    (stub, observer) -> stub.setDelay(localParams, observer),
                    responseObserver);
        } else {
            final QueueWithinCrawl qwc =
                    QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
            routeToOwner(
                    qwc,
                    observer -> super.setDelay(localParams, observer),
                    (stub, observer) -> stub.setDelay(localParams, observer),
                    responseObserver);
        }
    }

    /**
     * In cluster mode, a keyed request with local=false is routed to the node owning the queue. An
     * empty key keeps the historical local no-op instead of being routed to an artificial owner.
     * local=true always stays local.
     */
    @Override
    public void blockQueueUntil(BlockQueueParams request, StreamObserver<Empty> responseObserver) {
        if (request.getLocal() || !clusterMode || isClosing() || request.getKey().isEmpty()) {
            super.blockQueueUntil(request, responseObserver);
            return;
        }
        final QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
        final BlockQueueParams localParams =
                BlockQueueParams.newBuilder(request).setLocal(true).build();
        routeToOwner(
                qwc,
                observer -> super.blockQueueUntil(localParams, observer),
                (stub, observer) -> stub.blockQueueUntil(localParams, observer),
                responseObserver);
    }

    /**
     * In cluster mode, a request with local=false is applied locally and broadcast to every other
     * node. Concurrent setActive calls are not globally ordered: conflicting broadcasts may leave
     * nodes divergent even when each call reports success. local=true stays local. Unlike the other
     * overrides there is no isClosing() guard, because the base method changes the flag even while
     * closing.
     */
    @Override
    public void setActive(Active request, StreamObserver<Empty> responseObserver) {
        if (request.getLocal() || !clusterMode) {
            super.setActive(request, responseObserver);
            return;
        }
        final Active localParams = Active.newBuilder(request).setLocal(true).build();
        broadcastAll(
                observer -> super.setActive(localParams, observer),
                (stub, observer) -> stub.setActive(localParams, observer),
                responseObserver);
    }

    /** Create or return an existing stream to an external Frontier * */
    private final CacheLoader<Integer, StreamObserver<URLItem>> observerloader =
            new CacheLoader<>() {
                @Override
                public StreamObserver<URLItem> load(Integer index) {

                    final String nodeAddress = getNodes().get(index);

                    final URLFrontierStub stub =
                            URLFrontierGrpc.newStub(channelCache.getUnchecked(nodeAddress));

                    // pass an observer for the results coming back from that node
                    final StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                            observer =
                                    new StreamObserver<>() {

                                        @Override
                                        public void onNext(
                                                crawlercommons.urlfrontier.Urlfrontier.AckMessage
                                                        value) {

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
                                            // remove it whether we have been able to return the
                                            // value or not
                                            // this is an issue if 2 or more instances of the same
                                            // URL
                                            // are sent within a short period of time
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

        Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                getQueues().entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
            crawlIDs.add(e.getKey().getCrawlid());
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
        StreamObserver<AckMessage> sso = SynchronizedStreamObserver.wrapping(responseObserver, -1);

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
                int partition = partitionFor(qk, getNodes());

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
                                final Status s = putURLItem(value);
                                LOG.debug("Local putURL -> {} got status {}", url, s);
                                final AckMessage ackedMessage = ack.setStatus(s).build();
                                sso.onNext(ackedMessage);
                                unacked.decrementAndGet();
                            });
                } else {
                    // forward to non-local node
                    LOG.debug(
                            "Sending {} to partition {} -> {}",
                            url,
                            partition,
                            getNodes().get(partition));

                    // if the same ID is already being processed by
                    // a remote Frontier, just wait until it has been completed
                    while (inprocesscache.getIfPresent(ack.getID()) != null) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // set the flag back to <code>true
                        }
                    }

                    // store the tuple to return in a temporary cache
                    inprocesscache.put(ack.getID(), sso);

                    // get the stream observer for the node in charge of the partition
                    // and give it the value to process
                    observercache.getUnchecked(partition).onNext(value);
                }
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
                while (unacked.get() != 0 || inprocesscache.asMap().containsValue(sso)) {
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
