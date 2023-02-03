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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Builder;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.Boolean;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.Local;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

public abstract class AbstractFrontierService
        extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase
        implements Closeable {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(AbstractFrontierService.class);

    private static final Counter getURLs_calls =
            Counter.build()
                    .name("frontier_getURLs_calls_total")
                    .help("Number of times getURLs has been called.")
                    .register();

    private static final Counter getURLs_urls_count =
            Counter.build()
                    .name("frontier_getURLs_total")
                    .help("Number of URLs returned.")
                    .register();

    private static final Summary getURLs_Latency =
            Summary.build()
                    .name("frontier_getURLs_latency_seconds")
                    .help("getURLs latency in seconds.")
                    .register();

    protected static final Counter putURLs_calls =
            Counter.build()
                    .name("frontier_putURLs_calls_total")
                    .help("Number of times putURLs has been called.")
                    .register();

    protected static final Counter putURLs_urls_count =
            Counter.build()
                    .name("frontier_putURLs_total")
                    .help("Number of URLs sent to the Frontier")
                    .register();

    protected static final Counter putURLs_discovered_count =
            Counter.build()
                    .name("frontier_putURLs_discovered_total")
                    .help("Count of discovered URLs sent to the Frontier")
                    .labelNames("discovered")
                    .register();

    protected static final Counter putURLs_alreadyknown_count =
            Counter.build()
                    .name("frontier_putURLs_ignored_total")
                    .help("Number of discovered URLs already known to the Frontier")
                    .register();

    protected static final Counter putURLs_completed_count =
            Counter.build()
                    .name("frontier_putURLs_completed_total")
                    .help("Number of completed URLs")
                    .register();

    private boolean active = true;

    private int defaultDelayForQueues = 1;

    // used for reporting itself in a cluster setup
    protected final String address;

    // known nodes in a cluster setup
    private List<String> nodes;

    // in memory map of metadata for each queue
    private final Map<QueueWithinCrawl, QueueInterface> queues =
            Collections.synchronizedMap(new LinkedHashMap<>());

    protected final ExecutorService readExecutorService;
    protected final ExecutorService writeExecutorService;

    protected AbstractFrontierService(String host, int port) {
        this(Collections.emptyMap(), host, port);
    }

    protected AbstractFrontierService(
            final Map<String, String> configuration, String host, int port) {
        address = host + ":" + port;
        final int availableProcessor = Runtime.getRuntime().availableProcessors();
        LOG.info("Available processor(s) {}", availableProcessor);
        // by default uses 1/4 of the available processors
        final String defaultParallelism = Integer.toString(Math.max(availableProcessor / 4, 1));
        final int rthreadNum =
                Integer.parseInt(configuration.getOrDefault("read.thread.num", defaultParallelism));
        LOG.info("Using {} threads for reading from queues", rthreadNum);
        readExecutorService = Executors.newFixedThreadPool(rthreadNum);
        final int wthreadNum =
                Integer.parseInt(
                        configuration.getOrDefault("write.thread.num", defaultParallelism));
        writeExecutorService = Executors.newFixedThreadPool(wthreadNum);
        LOG.info("Using {} threads for writing to queues", wthreadNum);
    }

    public Map<QueueWithinCrawl, QueueInterface> getQueues() {
        return queues;
    }

    private boolean closing = false;

    protected boolean isClosing() {
        return closing;
    }

    public int getDefaultDelayForQueues() {
        return defaultDelayForQueues;
    }

    protected List<String> getNodes() {
        return nodes;
    }

    public void setDefaultDelayForQueues(int defaultDelayForQueues) {
        this.defaultDelayForQueues = defaultDelayForQueues;
    }

    protected boolean isActive() {
        return active;
    }

    public String getAddress() {
        return address;
    }

    public void setNodes(List<String> n) {
        nodes = n;
        Collections.sort(nodes);
        LOG.debug(address);
        int pos = 0;
        for (String node : nodes) {
            LOG.info("Node {}: {}", pos, node);
            pos++;
        }
    }

    public void createOrCleanDirectory(String path) {
        try {
            File file = new File(path);
            if (file.isDirectory()) {
                FileUtils.cleanDirectory(file);
            } else {
                FileUtils.forceMkdir(file);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void listCrawls(
            crawlercommons.urlfrontier.Urlfrontier.Local request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                    responseObserver) {

        Set<String> crawlIDs = new HashSet<>();

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

    @Override
    public void deleteCrawl(
            crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage crawlID,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
                    responseObserver) {
        long total = -1;

        if (!isClosing()) {
            total = 0;
            final String normalisedCrawlID = CrawlID.normaliseCrawlID(crawlID.getValue());

            final Set<QueueWithinCrawl> toDelete = new HashSet<>();

            synchronized (getQueues()) {
                Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                        getQueues().entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
                    QueueWithinCrawl qwc = e.getKey();
                    if (qwc.getCrawlid().equals(normalisedCrawlID)) {
                        toDelete.add(qwc);
                    }
                }

                for (QueueWithinCrawl quid : toDelete) {
                    QueueInterface q = getQueues().remove(quid);
                    total += q.countActive();
                }
            }
        }
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder().setValue(total).build());
        responseObserver.onCompleted();
    }

    @Override
    public void setActive(
            crawlercommons.urlfrontier.Urlfrontier.Active request,
            StreamObserver<Empty> responseObserver) {
        active = request.getState();
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getActive(Local request, StreamObserver<Boolean> responseObserver) {
        responseObserver.onNext(Boolean.newBuilder().setState(active).build());
        responseObserver.onCompleted();
    }

    protected String provideMissingKey(final String url) {
        String host = url;
        // find protocol part
        int protocolPos = host.indexOf("://");
        if (protocolPos != -1) {
            host = url.substring(protocolPos + 3);
        }

        int port = host.indexOf(":");
        if (port != -1) {
            host = host.substring(0, port);
        }

        int sep = host.indexOf("/");
        if (sep != -1) {
            host = host.substring(0, sep);
        }

        sep = host.indexOf("?");
        if (sep != -1) {
            host = host.substring(0, sep);
        }

        sep = host.indexOf("&");
        if (sep != -1) {
            host = host.substring(0, sep);
        }

        if (host.length() == 0) return null;

        return host;
    }

    @Override
    public void listQueues(
            crawlercommons.urlfrontier.Urlfrontier.Pagination request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.QueueList>
                    responseObserver) {

        long maxQueues = request.getSize();
        long start = request.getStart();

        boolean include_inactive = request.getIncludeInactive();

        final String normalisedCrawlID = CrawlID.normaliseCrawlID(request.getCrawlID());

        // 100 by default
        if (maxQueues == 0) {
            maxQueues = 100;
        }

        LOG.info(
                "Received request to list queues [size {}; start {}; inactive {}]",
                maxQueues,
                start,
                include_inactive);

        long now = Instant.now().getEpochSecond();
        int pos = -1;
        int sent = 0;

        crawlercommons.urlfrontier.Urlfrontier.QueueList.Builder list = QueueList.newBuilder();

        synchronized (getQueues()) {
            Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                    getQueues().entrySet().iterator();

            while (iterator.hasNext() && sent <= maxQueues) {
                Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
                pos++;

                // check that it is within the right crawlID
                if (!e.getKey().getCrawlid().equals(normalisedCrawlID)) {
                    continue;
                }

                // check that it isn't blocked
                if (!include_inactive && e.getValue().getBlockedUntil() >= now) {
                    continue;
                }

                // ignore the nextfetchdate
                if (include_inactive || e.getValue().countActive() > 0) {
                    if (pos >= start) {
                        list.addValues(e.getKey().getQueue());
                        sent++;
                    }
                }
            }
        }
        responseObserver.onNext(list.build());
        responseObserver.onCompleted();
    }

    @Override
    public void blockQueueUntil(BlockQueueParams request, StreamObserver<Empty> responseObserver) {
        if (!isClosing()) {
            QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
            QueueInterface queue = getQueues().get(qwc);
            if (queue != null) {
                queue.setBlockedUntil(request.getTime());
            }
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void setDelay(QueueDelayParams request, StreamObserver<Empty> responseObserver) {
        if (!isClosing()) {
            if (request.getKey().isEmpty()) {
                setDefaultDelayForQueues(request.getDelayRequestable());
            } else {
                QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
                QueueInterface queue = getQueues().get(qwc);
                if (queue != null) {
                    queue.setDelay(request.getDelayRequestable());
                }
            }
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    /**
     *
     *
     * <pre>
     * * Delete the queue based on the key in parameter *
     * </pre>
     */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
                    responseObserver) {
        long countActive = -1;
        if (!isClosing()) {
            QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
            QueueInterface q = getQueues().remove(qwc);
            countActive = q.countActive();
        }
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder()
                        .setValue(countActive)
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getStats(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<Stats> responseObserver) {
        LOG.info("Received stats request");

        final Map<String, Long> s = new HashMap<>();

        int inProc = 0;
        int numQueues = 0;
        int size = 0;
        long completed = 0;
        long activeQueues = 0;

        List<QueueInterface> _queues = new LinkedList<>();

        final String normalisedCrawlID = CrawlID.normaliseCrawlID(request.getCrawlID());

        // specific queue?
        if (!request.getKey().isEmpty()) {
            QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
            QueueInterface q = getQueues().get(qwc);
            if (q != null) {
                _queues.add(q);
            } else {
                // TODO notify an error to the client
            }
        }
        // all the queues within the crawlID
        else {

            synchronized (getQueues()) {
                // check that the queues belong to the crawlid specified
                Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                        getQueues().entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
                    QueueWithinCrawl qwc = e.getKey();
                    if (qwc.getCrawlid().equals(normalisedCrawlID)) {
                        _queues.add(e.getValue());
                    }
                }
            }
        }
        // backed by the queues so can result in a
        // ConcurrentModificationException

        long now = Instant.now().getEpochSecond();

        synchronized (getQueues()) {
            for (QueueInterface q : _queues) {
                final int inProcForQ = q.getInProcess(now);
                final int activeForQ = q.countActive();
                if (inProcForQ > 0 || activeForQ > 0) {
                    activeQueues++;
                }
                inProc += inProcForQ;
                numQueues++;
                size += activeForQ;
                completed += q.getCountCompleted();
            }
        }

        // put count completed as custom stats for now
        // add it as a proper field later?
        s.put("completed", completed);
        // same for active_queues
        s.put("active_queues", activeQueues);

        Stats stats =
                Stats.newBuilder()
                        .setNumberOfQueues(numQueues)
                        .setSize(size)
                        .setInProcess(inProc)
                        .putAllCounts(s)
                        .setCrawlID(normalisedCrawlID)
                        .build();
        responseObserver.onNext(stats);
        responseObserver.onCompleted();
    }

    @Override
    public void getURLs(GetParams request, StreamObserver<URLInfo> responseObserver) {
        // on hold or shutting down
        if (!isActive() || isClosing()) {
            responseObserver.onCompleted();
            return;
        }

        getURLs_calls.inc();

        UUID requestID = UUID.randomUUID();

        Summary.Timer requestTimer = getURLs_Latency.startTimer();

        final int maxQueues;

        // 0 by default
        if (request.getMaxQueues() == 0) {
            maxQueues = Integer.MAX_VALUE;
        } else {
            maxQueues = request.getMaxQueues();
        }

        final int maxURLsPerQueue;

        if (request.getMaxUrlsPerQueue() == 0) {
            maxURLsPerQueue = Integer.MAX_VALUE;
        } else {
            maxURLsPerQueue = request.getMaxUrlsPerQueue();
        }

        final int secsUntilRequestable;

        if (request.getDelayRequestable() == 0) {
            secsUntilRequestable = 30;
        } else {
            secsUntilRequestable = request.getDelayRequestable();
        }

        LOG.info(
                "Received request to get fetchable URLs [max queues {}, max URLs {}, delay {}] {}",
                maxQueues,
                maxURLsPerQueue,
                secsUntilRequestable,
                requestID.toString());

        long start = System.currentTimeMillis();

        // if null -> don't care about a particular crawl
        String crawlID = null;

        // default is an empty string
        if (request.hasCrawlID()) crawlID = request.getCrawlID();

        String key = request.getKey();

        long now = Instant.now().getEpochSecond();

        final SynchronizedStreamObserver<URLInfo> synchStreamObs =
                (SynchronizedStreamObserver<URLInfo>)
                        SynchronizedStreamObserver.wrapping(responseObserver, maxQueues);

        // want a specific key only?
        // default is an empty string so should never be null
        if (key != null && key.length() >= 1) {

            // if want a specific queue - must be in a crawlID
            // even the default one

            if (crawlID == null) {
                LOG.error("Want URLs from a specific queue but the crawlID is not set");
                synchStreamObs.onCompleted();
                return;
            }

            QueueWithinCrawl qwc = QueueWithinCrawl.get(key, crawlID);
            QueueInterface queue = getQueues().get(qwc);

            // the queue does not exist
            if (queue == null) {
                synchStreamObs.onCompleted();
                return;
            }

            // it is locked
            if (queue.getBlockedUntil() >= now) {
                synchStreamObs.onCompleted();
                return;
            }

            // too early?
            int delay = queue.getDelay();
            if (delay == -1) delay = getDefaultDelayForQueues();
            if (queue.getLastProduced() + delay >= now) {
                responseObserver.onCompleted();
                return;
            }

            int totalSent =
                    sendURLsForQueue(
                            queue, qwc, maxURLsPerQueue, secsUntilRequestable, now, synchStreamObs);
            responseObserver.onCompleted();

            getURLs_urls_count.inc(totalSent);

            LOG.info(
                    "Sent {} from queue {} in {} msec {}",
                    totalSent,
                    key,
                    (System.currentTimeMillis() - start),
                    requestID.toString());

            if (totalSent != 0) {
                queue.setLastProduced(now);
            }

            requestTimer.observeDuration();

            return;
        }

        final AtomicInteger numQueuesTried = new AtomicInteger();
        final AtomicInteger numQueuesSent = new AtomicInteger();
        final AtomicInteger totalSent = new AtomicInteger();
        final AtomicInteger inProcess = new AtomicInteger();

        QueueWithinCrawl firstCrawlQueue = null;

        if (getQueues().isEmpty()) {
            LOG.info("No queues to get URLs from! {}", requestID.toString());
            responseObserver.onCompleted();
            return;
        }

        while (numQueuesSent.get() < maxQueues) {

            final QueueInterface currentQueue;
            final QueueWithinCrawl currentCrawlQueue;

            synchronized (getQueues()) {
                Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                        getQueues().entrySet().iterator();
                Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
                currentQueue = e.getValue();
                currentCrawlQueue = e.getKey();

                // to make sure we don't loop over the ones we already processed
                if (firstCrawlQueue == null) {
                    firstCrawlQueue = currentCrawlQueue;
                } else if (firstCrawlQueue.equals(currentCrawlQueue)) {
                    break;
                }
                // We remove the entry and put it at the end of the map
                iterator.remove();
                getQueues().put(currentCrawlQueue, currentQueue);
            }

            // if a crawlID has been specified make sure it matches
            if (crawlID != null && !currentCrawlQueue.getCrawlid().equals(crawlID)) {
                continue;
            }

            // it is locked
            if (currentQueue.getBlockedUntil() >= now) {
                continue;
            }

            // too early?
            int delay = currentQueue.getDelay();
            if (delay == -1) delay = getDefaultDelayForQueues();
            if (currentQueue.getLastProduced() + delay >= now) {
                continue;
            }

            // already has its fill of URLs in process
            if (currentQueue.getInProcess(now) >= maxURLsPerQueue) {
                continue;
            }

            inProcess.incrementAndGet();

            readExecutorService.execute(
                    () -> {
                        final int sentForQ =
                                sendURLsForQueue(
                                        currentQueue,
                                        currentCrawlQueue,
                                        maxURLsPerQueue,
                                        secsUntilRequestable,
                                        now,
                                        synchStreamObs);

                        if (sentForQ > 0) {
                            currentQueue.setLastProduced(now);
                            totalSent.addAndGet(sentForQ);
                            numQueuesSent.incrementAndGet();
                        }

                        inProcess.decrementAndGet();
                        numQueuesTried.incrementAndGet();
                    });
        }

        // wait for all threads to have finished
        while (inProcess.get() != 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        LOG.info(
                "Sent {} from {} queue(s) in {} msec; tried {} queues. {}",
                totalSent,
                numQueuesSent,
                (System.currentTimeMillis() - start),
                numQueuesTried,
                requestID.toString());

        getURLs_urls_count.inc(totalSent.get());

        requestTimer.observeDuration();

        synchStreamObs.onCompleted();
    }

    protected abstract int sendURLsForQueue(
            QueueInterface queue,
            QueueWithinCrawl key,
            int maxURLsPerQueue,
            int secsUntilRequestable,
            long now,
            SynchronizedStreamObserver<URLInfo> responseObserver);

    @Override
    public void setLogLevel(LogLevelParams request, StreamObserver<Empty> responseObserver) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(request.getPackage());
        logger.setLevel(Level.toLevel(request.getLevel().toString()));
        LOG.info("Log level for {} set to {}", request.getPackage(), request.getLevel().toString());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void listNodes(Empty request, StreamObserver<StringList> responseObserver) {

        if (nodes == null) {
            nodes = new ArrayList<>();
        }

        // by default return only this node.
        if (nodes.isEmpty()) {
            nodes.add(this.getAddress());
        }
        responseObserver.onNext(StringList.newBuilder().addAllValues(nodes).build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<URLItem> putURLs(
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage> responseObserver) {

        putURLs_calls.inc();

        StreamObserver<crawlercommons.urlfrontier.Urlfrontier.AckMessage> sso =
                SynchronizedStreamObserver.wrapping(responseObserver, -1);

        return new StreamObserver<URLItem>() {

            final AtomicInteger unacked = new AtomicInteger();

            @Override
            public void onNext(URLItem value) {
                String url;
                if (value.hasDiscovered()) {
                    url = value.getDiscovered().getInfo().getUrl();
                } else {
                    url = value.getKnown().getInfo().getUrl();
                }

                final Builder ack = AckMessage.newBuilder();
                if (value.getID() == null || value.getID().isEmpty()) {
                    ack.setID(url);
                } else {
                    ack.setID(value.getID());
                }

                // do not add new stuff if we are in the process of closing
                if (isClosing()) {
                    sso.onNext(ack.setStatus(Status.FAIL).build());
                    return;
                }

                unacked.incrementAndGet();

                writeExecutorService.execute(
                        () -> {
                            final Status status = putURLItem(value);
                            LOG.debug("putURL -> {} got status {}", url, status);
                            final AckMessage ackedMessage = ack.setStatus(status).build();
                            sso.onNext(ackedMessage);
                            unacked.decrementAndGet();
                        });
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
                LOG.error("Error reported {}", t.getMessage());
            }

            @Override
            public void onCompleted() {
                // will this ever get called if the client is constantly streaming?
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

    /**
     * logic for handling an individual item, returns a status indicating whether the addition has
     * succeeded
     */
    protected abstract AckMessage.Status putURLItem(URLItem value);

    @Override
    public void close() throws IOException {
        closing = true;
        writeExecutorService.shutdown();
        readExecutorService.shutdown();
        try {
            if (!writeExecutorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                writeExecutorService.shutdownNow();
            }
            if (!readExecutorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                readExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            writeExecutorService.shutdownNow();
            readExecutorService.shutdownNow();
        }
    }
}
