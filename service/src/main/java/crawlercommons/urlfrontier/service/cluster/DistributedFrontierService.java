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
import crawlercommons.urlfrontier.Urlfrontier.Boolean;
import crawlercommons.urlfrontier.Urlfrontier.CrawlLimitParams;
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
import crawlercommons.urlfrontier.service.AsyncCompletion;
import crawlercommons.urlfrontier.service.ParamHelper;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.SynchronizedStreamObserver;
import io.grpc.Context;
import io.grpc.Deadline;
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
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

public abstract class DistributedFrontierService extends AbstractFrontierService {

    public DistributedFrontierService(
            final Map<String, String> configuration, String host, int port) {
        super(configuration, host, port);
        forwardDeadlineSeconds =
                ParamHelper.getIntegerParameter(
                        configuration, "forward.deadline.seconds", FORWARD_DEADLINE_SECONDS);
    }

    // no explicit config
    public DistributedFrontierService(String host, int port) {
        this(new HashMap<String, String>(), host, port);
    }

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(DistributedFrontierService.class);

    protected boolean clusterMode = false;

    /** Default for {@link #forwardDeadlineSeconds}. */
    static final int FORWARD_DEADLINE_SECONDS = 30;

    /**
     * Maximum time granted to a forwarded control call before failing the caller. Configurable with
     * 'forward.deadline.seconds'.
     */
    private final int forwardDeadlineSeconds;

    /** How often the items forwarded to other nodes are checked for expiry. */
    static final int INPROCESS_CLEANUP_SECONDS = 10;

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

    /**
     * A deadline is mandatory on forwarded blocking calls: without one an unresponsive node holds
     * the caller's gRPC handler thread for ever, and since each of these calls is a fan-out one bad
     * node takes down the cluster-wide view for every client that asks. Callers pass a single
     * deadline for the whole fan-out rather than one per hop, so that querying N nodes is bounded
     * once instead of N times.
     */
    private URLFrontierBlockingStub getFrontier(String target, Deadline deadline) {
        return URLFrontierGrpc.newBlockingStub(channelCache.getUnchecked(target))
                .withDeadline(deadline);
    }

    /** Deadline shared by every hop of a fan-out, see {@link #getFrontier(String, Deadline)}. */
    private Deadline forwardDeadline() {
        return Deadline.after(forwardDeadlineSeconds, TimeUnit.SECONDS);
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
                .withDeadlineAfter(forwardDeadlineSeconds, TimeUnit.SECONDS);
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
     * In cluster mode, a keyed request with local=false is routed to the node owning the queue. An
     * empty key is invalid for this keyed-only operation and fails locally. local=true always stays
     * local.
     */
    @Override
    public void setCrawlLimit(CrawlLimitParams request, StreamObserver<Empty> responseObserver) {
        if (request.getLocal() || !clusterMode || isClosing() || request.getKey().isEmpty()) {
            super.setCrawlLimit(request, responseObserver);
            return;
        }
        final QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
        // forward the normalized crawlID so owners that don't normalize internally still match
        final CrawlLimitParams localParams =
                CrawlLimitParams.newBuilder(request)
                        .setCrawlID(qwc.getCrawlid())
                        .setLocal(true)
                        .build();
        routeToOwner(
                qwc,
                observer -> super.setCrawlLimit(localParams, observer),
                (stub, observer) -> stub.setCrawlLimit(localParams, observer),
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

    /**
     * In cluster mode, local=false returns the AND of every node's state: true only if all nodes
     * are active. A mixed state returns false and logs a warning. This is not an atomic snapshot:
     * it aggregates the states observed while querying the nodes, under a single shared deadline.
     * An unreachable node surfaces an error rather than a fabricated false. local=true returns the
     * state of this node only.
     */
    @Override
    public void getActive(Local request, StreamObserver<Boolean> responseObserver) {
        if (request.getLocal() || !clusterMode) {
            super.getActive(request, responseObserver);
            return;
        }
        final List<String> nodes = List.copyOf(getNodes());
        if (nodes.indexOf(address) == -1) {
            throw new RuntimeException(
                    "Found conf 'nodes' but current node's address not in the list");
        }
        // one deadline for the whole aggregation, not per node
        final Deadline deadline = forwardDeadline();
        final Local localRequest = Local.newBuilder().setLocal(true).build();
        final boolean localState = isActive();
        boolean allActive = localState;
        boolean sawActive = localState;
        boolean sawInactive = !localState;
        try {
            for (String node : nodes) {
                if (node.equals(address)) {
                    continue;
                }
                // &= evaluates the right-hand side regardless: every node is
                // queried, an unreachable one fails the call instead of being
                // silently folded into a false
                final boolean nodeActive =
                        getFrontier(node, deadline).getActive(localRequest).getState();
                allActive &= nodeActive;
                sawActive |= nodeActive;
                sawInactive |= !nodeActive;
            }
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
            return;
        }
        if (sawActive && sawInactive) {
            LOG.warn("URLFrontier nodes report divergent active states");
        }
        responseObserver.onNext(Boolean.newBuilder().setState(allActive).build());
        responseObserver.onCompleted();
    }

    /** Create or return an existing stream to an external Frontier * */
    private final CacheLoader<Integer, StreamObserver<URLItem>> observerloader =
            new CacheLoader<>() {
                @Override
                public StreamObserver<URLItem> load(Integer index) {

                    final String nodeAddress = getNodes().get(index);

                    final URLFrontierStub stub =
                            URLFrontierGrpc.newStub(channelCache.getUnchecked(nodeAddress));

                    // the stream removes itself from the cache when it ends; it is held
                    // here so that only this stream is ever removed and never a
                    // replacement created for the same partition in the meantime
                    final AtomicReference<StreamObserver<URLItem>> self = new AtomicReference<>();

                    // pass an observer for the results coming back from that node
                    final StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                            observer =
                                    new StreamObserver<>() {

                                        @Override
                                        public void onNext(
                                                crawlercommons.urlfrontier.Urlfrontier.AckMessage
                                                        value) {

                                            // the ID carried by the ack is the correlation
                                            // token we generated when forwarding the item;
                                            // remove it whether we can return the value or not
                                            final PendingAck pending =
                                                    inprocesscache.asMap().remove(value.getID());

                                            if (pending == null) {
                                                LOG.error(
                                                        "No stream found to ack back for {} with status {}",
                                                        value.getID(),
                                                        value.getStatus());
                                                return;
                                            }

                                            LOG.debug(
                                                    "Got stream to ack back for {} with status {}",
                                                    pending.clientID,
                                                    value.getStatus());

                                            // go back to the client and notify that it has
                                            // worked, under the ID the client knows about
                                            pending.ack(value.getStatus());
                                        }

                                        @Override
                                        public void onError(Throwable t) {
                                            discardForwardingStream(index, self.get());
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
                                            // the remote side will not ack anything else on
                                            // this stream: it must not be handed out again
                                            discardForwardingStream(index, self.get());
                                        }
                                    };

                    // this stream is shared by every client stream forwarding to that
                    // node, so it must not inherit the Context of whichever server call
                    // happened to trigger the load: that call ending would cancel the
                    // stream for everyone and strand the items already in flight on it
                    final StreamObserver<URLItem> forwarder;
                    final Context previous = Context.ROOT.attach();
                    try {
                        // gRPC serializes the callbacks of a single stream but not across
                        // streams, and concurrent onNext on one client call corrupts its
                        // outbound buffers; -1 disables the token budget, the wrapper is
                        // used here for its mutual exclusion only
                        forwarder = SynchronizedStreamObserver.wrapping(stub.putURLs(observer), -1);
                    } finally {
                        Context.ROOT.detach(previous);
                    }
                    self.set(forwarder);
                    return forwarder;
                }
            };

    /**
     * Removing a stream from the cache closes it: the call is half-closed, so the node keeps the
     * items already forwarded on it and acks them back on the response side, which stays open until
     * it has answered them all. Dropping the entry without this leaves the call half-open on both
     * nodes until the channel is shut down (issue #207).
     */
    private final RemovalListener<Integer, StreamObserver<URLItem>> observerlistener =
            new RemovalListener<>() {
                @Override
                public void onRemoval(RemovalNotification<Integer, StreamObserver<URLItem>> n) {
                    LOG.info("Removed StreamObserver {} with key {}", n.getValue(), n.getKey());
                    final StreamObserver<URLItem> observer = n.getValue();
                    if (observer == null) {
                        return;
                    }
                    try {
                        observer.onCompleted();
                    } catch (RuntimeException e) {
                        // a stream is usually discarded *because* it has already failed
                        LOG.debug(
                                "Error while closing the stream forwarding to partition {}: {}",
                                n.getKey(),
                                e.getLocalizedMessage());
                    }
                }
            };

    /**
     * One forwarding stream per partition, held for the lifetime of the service. There is no expiry
     * on purpose: the node list is fixed at startup so there is nothing to reclaim by dropping
     * streams, and a stream cannot be closed on a timer without stranding the items it still has in
     * flight or racing with a concurrent onNext (issue #207). An entry is removed only once its
     * stream has ended - the remote side completing or erroring, or a failed onNext - and by {@link
     * #close()}, so a broken stream is never handed out for long.
     */
    private final LoadingCache<Integer, StreamObserver<URLItem>> observercache =
            CacheBuilder.newBuilder().removalListener(observerlistener).build(observerloader);

    /**
     * Discards a forwarding stream, closing it, but only if it is still the one held for that
     * partition: a stream that has ended must not take away the replacement another thread may have
     * created for the same partition in the meantime. A null observer means the stream ended before
     * the load which created it returned, in which case there is nothing cached to remove yet - the
     * first item forwarded on it fails and discards it then.
     */
    private void discardForwardingStream(int partition, StreamObserver<URLItem> observer) {
        if (observer == null) {
            return;
        }
        observercache.asMap().remove(partition, observer);
    }

    /**
     * Discards the forwarding stream held for a partition, if any. Visible for testing: in
     * production the streams are discarded by {@link #discardForwardingStream(int, StreamObserver)}
     * when they end, or by {@link #close()}.
     */
    void discardForwardingStream(int partition) {
        observercache.invalidate(partition);
    }

    /**
     * An item forwarded to another node, waiting for that node to ack it back. Held in {@link
     * #inprocesscache} under a correlation token: the ID given by the client can be repeated within
     * a stream or across streams, a token never is.
     */
    private static final class PendingAck {

        private final String clientID;

        /** the synchronized observer of the client stream the item came from */
        private final StreamObserver<AckMessage> client;

        /** completion of that stream: this item is one of its outstanding tasks */
        private final AsyncCompletion completion;

        private final AtomicBoolean acked = new AtomicBoolean();

        PendingAck(String clientID, StreamObserver<AckMessage> client, AsyncCompletion completion) {
            this.clientID = clientID;
            this.client = client;
            this.completion = completion;
        }

        /**
         * Sends the status back to the client under its own ID and releases the item. Subsequent
         * calls do nothing: the ack coming back from the remote node can race with the eviction of
         * an item given up on.
         */
        void ack(Status status) {
            if (!acked.compareAndSet(false, true)) {
                return;
            }
            try {
                client.onNext(AckMessage.newBuilder().setID(clientID).setStatus(status).build());
            } catch (Exception e) {
                LOG.error(
                        "Error while communicating back with the client: {} ",
                        e.getLocalizedMessage());
            } finally {
                completion.taskDone();
            }
        }
    }

    private final RemovalListener<String, PendingAck> inProcessRemovalListener =
            new RemovalListener<>() {
                @Override
                public void onRemoval(RemovalNotification<String, PendingAck> notification) {
                    // try to notify the client if something was sent to another instance but never
                    // came back?
                    if (notification.wasEvicted()) {
                        PendingAck pending = notification.getValue();
                        if (pending != null) {
                            LOG.debug(
                                    "Trying to notify original stream about eviction of {}",
                                    pending.clientID);
                            pending.ack(Status.FAIL);
                        }
                    }
                }
            };

    private Cache<String, PendingAck> inprocesscache =
            CacheBuilder.newBuilder()
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .removalListener(inProcessRemovalListener)
                    .build();

    /**
     * Guava evicts only during cache activity: without a heartbeat, a stream whose remote acks
     * never come back would stay open instead of being failed by the expiry above.
     */
    private final ScheduledExecutorService inprocessCacheCleaner =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "inprocesscache-cleanup");
                        t.setDaemon(true);
                        return t;
                    });

    {
        inprocessCacheCleaner.scheduleWithFixedDelay(
                () -> {
                    try {
                        inprocesscache.cleanUp();
                    } catch (Exception e) {
                        LOG.error("Exception while cleaning up the in-process cache", e);
                    }
                },
                INPROCESS_CLEANUP_SECONDS,
                INPROCESS_CLEANUP_SECONDS,
                TimeUnit.SECONDS);
    }

    /** Correlation tokens for the items forwarded to other nodes; unique within this instance. */
    private final AtomicLong correlationSequence = new AtomicLong();

    /**
     * Delete the queue based on the key in parameter. In cluster mode the deletion is fanned out to
     * every other node under a single deadline; an unreachable node fails the call instead of
     * silently returning a partial count. The operation is not atomic: the nodes reached may have
     * deleted their share when an error is returned, but repeating the deletion is idempotent.
     */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long> responseObserver) {

        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());

        int sizeQueue = 0;

        if (!request.getLocal() && clusterMode) {
            // force to local so that remote node don't go recursive
            QueueWithinCrawlParams local =
                    QueueWithinCrawlParams.newBuilder(request).setLocal(true).build();
            final Deadline deadline = forwardDeadline();
            try {
                for (String node : getNodes()) {
                    if (node.equals(address)) continue;
                    // call the delete endpoint in the target node
                    crawlercommons.urlfrontier.Urlfrontier.Long total =
                            getFrontier(node, deadline).deleteQueue(local);
                    sizeQueue += total.getValue();
                }
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
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

    /**
     * Same fan-out contract as {@link #deleteQueue}: bounded by a single deadline, an unreachable
     * node fails the call rather than under-reporting the number of URLs deleted.
     */
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
            final Deadline deadline = forwardDeadline();
            try {
                for (String node : getNodes()) {
                    if (node.equals(address)) continue;
                    // call the delete endpoint in the target node
                    crawlercommons.urlfrontier.Urlfrontier.Long localCount =
                            getFrontier(node, deadline).deleteCrawl(local);
                    total += localCount.getValue();
                }
            } catch (StatusRuntimeException e) {
                responseObserver.onError(e);
                return;
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
        // one deadline for the whole aggregation, not per node
        final Deadline deadline = forwardDeadline();
        try {
            for (String node : getNodes()) {
                Stats localStats = getFrontier(node, deadline).getStats(local);
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
        } catch (StatusRuntimeException e) {
            // an unreachable node surfaces an error rather than silently deflating the
            // cluster-wide figures
            responseObserver.onError(e);
            return;
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
            final Deadline deadline = forwardDeadline();
            try {
                for (String node : getNodes()) {
                    // exclude the local node
                    if (node.equals(address)) continue;
                    getFrontier(node, deadline).setLogLevel(local);
                }
            } catch (StatusRuntimeException e) {
                // the level is not applied locally either: the call is idempotent, a retry
                // once the node is back sets it everywhere
                responseObserver.onError(e);
                return;
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
            // one deadline for the whole aggregation, not per node
            final Deadline deadline = forwardDeadline();
            try {
                for (String node : getNodes()) {
                    // exclude the local node
                    if (node.equals(address)) continue;
                    StringList results = getFrontier(node, deadline).listCrawls(local);
                    for (String s : results.getValuesList()) {
                        crawlIDs.add(s);
                    }
                }
            } catch (StatusRuntimeException e) {
                // an unreachable node surfaces an error rather than an incomplete list
                responseObserver.onError(e);
                return;
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
        // one deadline for the whole aggregation, not per node
        final Deadline deadline = forwardDeadline();
        try {
            for (String node : getNodes()) {
                QueueList listqueues = getFrontier(node, deadline).listQueues(localPagination);
                for (String s : listqueues.getValuesList()) {
                    dedup.add(s);
                }
            }
        } catch (StatusRuntimeException e) {
            // an unreachable node surfaces an error rather than an incomplete list
            responseObserver.onError(e);
            return;
        }

        crawlercommons.urlfrontier.Urlfrontier.QueueList.Builder list = QueueList.newBuilder();
        list.addAllValues(dedup);
        responseObserver.onNext(list.build());
        responseObserver.onCompleted();
    }

    @Override
    public void close() throws IOException {
        super.close();
        inprocessCacheCleaner.shutdownNow();
        // half-close the forwarding streams before the channels go, so that the items
        // still in flight on them have a chance of being acked back
        observercache.invalidateAll();
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

        // wrap the response observer as a synchronized one; the in-flight budget
        // counts items forwarded to other nodes too, since their ack - and therefore
        // the next request - only comes once the remote node has answered
        StreamObserver<AckMessage> sso =
                SynchronizedStreamObserver.wrapping(
                        limitInFlight(responseObserver, putURLsMaxInFlight), -1);

        return new StreamObserver<URLItem>() {

            // closes the response once the client has stopped sending and every item has
            // been written locally or acked back by the node it was forwarded to
            final AsyncCompletion completion = new AsyncCompletion(sso::onCompleted);

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
                    completion.taskStarted();

                    try {
                        writeExecutorService.execute(
                                () -> {
                                    try {
                                        final Status s = putURLItem(value);
                                        LOG.debug("Local putURL -> {} got status {}", url, s);
                                        final AckMessage ackedMessage = ack.setStatus(s).build();
                                        sso.onNext(ackedMessage);
                                    } finally {
                                        // whatever happens the item must be released or the
                                        // response is never closed
                                        completion.taskDone();
                                    }
                                });
                    } catch (RejectedExecutionException e) {
                        // the task never started: tell the client instead of leaving it waiting
                        LOG.error("Executor rejected putURLs task for {}", url, e);
                        sso.onNext(ack.setStatus(Status.FAIL).build());
                        completion.taskDone();
                    }
                } else {
                    // forward to non-local node
                    LOG.debug(
                            "Sending {} to partition {} -> {}",
                            url,
                            partition,
                            getNodes().get(partition));

                    // the item goes out under a correlation token of ours rather than the
                    // client's ID: the same ID can legitimately be sent more than once, in
                    // which case the acks could no longer be told apart
                    final String token = Long.toString(correlationSequence.incrementAndGet());
                    final PendingAck pending = new PendingAck(ack.getID(), sso, completion);

                    completion.taskStarted();

                    // store the tuple to return in a temporary cache; must be in place
                    // before the item goes out as the ack can come back at any point
                    inprocesscache.put(token, pending);

                    StreamObserver<URLItem> forwarder = null;
                    try {
                        // get the stream observer for the node in charge of the partition
                        // and give it the value to process
                        forwarder = observercache.getUnchecked(partition);
                        forwarder.onNext(URLItem.newBuilder(value).setID(token).build());
                    } catch (Exception e) {
                        LOG.error(
                                "Error while sending {} to partition {}: {}",
                                url,
                                partition,
                                e.getLocalizedMessage());
                        // no ack will ever come back for it: fail it now instead of
                        // waiting for the entry to expire
                        discardForwardingStream(partition, forwarder);
                        inprocesscache.invalidate(token);
                        pending.ack(Status.FAIL);
                    }
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
                // the response is closed here if all the work for this stream has already
                // ended, or by the last item to be acked otherwise
                completion.noMoreTasks();
            }
        };
    }

    protected abstract Status putURLItem(URLItem item);

    /**
     * A batch typically spans several partitions and would have to be split and re-batched per
     * node, which is not implemented yet: clients fall back to the per-URL stream instead.
     */
    @Override
    public io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.DiscoveredBatch>
            putDiscovered(
                    io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.BatchAck>
                            responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asRuntimeException());
        return new io.grpc.stub.StreamObserver<>() {
            @Override
            public void onNext(crawlercommons.urlfrontier.Urlfrontier.DiscoveredBatch value) {}

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {}
        };
    }
}
