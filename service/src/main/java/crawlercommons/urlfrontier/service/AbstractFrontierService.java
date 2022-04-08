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
import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.Boolean;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import io.grpc.stub.StreamObserver;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;
import java.time.Instant;
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
import org.slf4j.LoggerFactory;

public abstract class AbstractFrontierService
        extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase {

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

    // in memory map of metadata for each queue
    protected final Map<QueueWithinCrawl, QueueInterface> queues =
            Collections.synchronizedMap(new LinkedHashMap<>());

    public int getDefaultDelayForQueues() {
        return defaultDelayForQueues;
    }

    public void setDefaultDelayForQueues(int defaultDelayForQueues) {
        this.defaultDelayForQueues = defaultDelayForQueues;
    }

    protected boolean isActive() {
        return active;
    }

    @Override
    public void listCrawls(
            crawlercommons.urlfrontier.Urlfrontier.Empty request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.StringList>
                    responseObserver) {

        Set<String> crawlIDs = new HashSet<>();

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

    @Override
    public void deleteCrawl(
            crawlercommons.urlfrontier.Urlfrontier.String crawlID,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>
                    responseObserver) {

        long total = 0;

        final String normalisedCrawlID = CrawlID.normaliseCrawlID(crawlID.getValue());

        final Set<QueueWithinCrawl> toDelete = new HashSet<>();

        synchronized (queues) {
            Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                    queues.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<QueueWithinCrawl, QueueInterface> e = iterator.next();
                QueueWithinCrawl qwc = e.getKey();
                if (qwc.getCrawlid().equals(normalisedCrawlID)) {
                    toDelete.add(qwc);
                }
            }

            for (QueueWithinCrawl quid : toDelete) {
                QueueInterface q = queues.remove(quid);
                total += q.countActive();
            }
        }
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder()
                        .setValue(total)
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void setActive(
            crawlercommons.urlfrontier.Urlfrontier.Boolean request,
            StreamObserver<Empty> responseObserver) {
        active = request.getState();
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getActive(Empty request, StreamObserver<Boolean> responseObserver) {
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

        synchronized (queues) {
            Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                    queues.entrySet().iterator();

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
        QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
        QueueInterface queue = queues.get(qwc);
        if (queue != null) {
            queue.setBlockedUntil(request.getTime());
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void setDelay(QueueDelayParams request, StreamObserver<Empty> responseObserver) {
        if (request.getKey().isEmpty()) {
            setDefaultDelayForQueues(request.getDelayRequestable());
        } else {
            QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
            QueueInterface queue = queues.get(qwc);
            if (queue != null) {
                queue.setDelay(request.getDelayRequestable());
            }
        }
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    /**
     *
     *
     * <pre>
     * * Delete  the queue based on the key in parameter *
     * </pre>
     */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer>
                    responseObserver) {
        QueueWithinCrawl qwc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
        QueueInterface q = queues.remove(qwc);
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder()
                        .setValue(q.countActive())
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
            QueueInterface q = queues.get(qwc);
            if (q != null) {
                _queues.add(q);
            } else {
                // TODO notify an error to the client
            }
        }
        // all the queues within the crawlID
        else {

            synchronized (queues) {
                // check that the queues belong to the crawlid specified
                Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                        queues.entrySet().iterator();
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

        synchronized (queues) {
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
        // on hold
        if (!isActive()) {
            responseObserver.onCompleted();
            return;
        }

        getURLs_calls.inc();

        Summary.Timer requestTimer = getURLs_Latency.startTimer();

        int maxQueues = request.getMaxQueues();
        int maxURLsPerQueue = request.getMaxUrlsPerQueue();
        int secsUntilRequestable = request.getDelayRequestable();

        // 0 by default
        if (maxQueues == 0) {
            maxQueues = Integer.MAX_VALUE;
        }

        if (maxURLsPerQueue == 0) {
            maxURLsPerQueue = Integer.MAX_VALUE;
        }

        if (secsUntilRequestable == 0) {
            secsUntilRequestable = 30;
        }

        LOG.info(
                "Received request to get fetchable URLs [max queues {}, max URLs {}, delay {}]",
                maxQueues,
                maxURLsPerQueue,
                secsUntilRequestable);

        long start = System.currentTimeMillis();

        // if null -> don't care about a particular crawl
        String crawlID = null;

        // default is an empty string
        if (request.hasCrawlID()) crawlID = request.getCrawlID();

        String key = request.getKey();

        long now = Instant.now().getEpochSecond();

        // want a specific key only?
        // default is an empty string so should never be null
        if (key != null && key.length() >= 1) {

            // if want a specific queue - must be in a crawlID
            // even the default one

            if (crawlID == null) {
                // TODO log error
                responseObserver.onCompleted();
                return;
            }

            QueueWithinCrawl qwc = QueueWithinCrawl.get(key, crawlID);
            QueueInterface queue = queues.get(qwc);

            // the queue does not exist
            if (queue == null) {
                responseObserver.onCompleted();
                return;
            }

            // it is locked
            if (queue.getBlockedUntil() >= now) {
                responseObserver.onCompleted();
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
                            queue,
                            qwc,
                            maxURLsPerQueue,
                            secsUntilRequestable,
                            now,
                            responseObserver);
            responseObserver.onCompleted();

            getURLs_urls_count.inc(totalSent);

            LOG.info(
                    "Sent {} from queue {} in {} msec",
                    totalSent,
                    key,
                    (System.currentTimeMillis() - start));

            if (totalSent != 0) {
                queue.setLastProduced(now);
            }

            requestTimer.observeDuration();

            return;
        }

        int numQueuesTried = 0;
        int numQueuesSent = 0;
        int totalSent = 0;
        QueueWithinCrawl firstCrawlQueue = null;

        if (queues.isEmpty()) {
            LOG.info("No queues to get URLs from!");
            responseObserver.onCompleted();
            return;
        }

        while (numQueuesSent < maxQueues) {

            QueueInterface currentQueue = null;
            QueueWithinCrawl currentCrawlQueue = null;

            synchronized (queues) {
                numQueuesTried++;
                Iterator<Entry<QueueWithinCrawl, QueueInterface>> iterator =
                        queues.entrySet().iterator();
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
                queues.put(currentCrawlQueue, currentQueue);
            }

            // if a crawlID has been specified make sure it matches
            if (crawlID != null && !currentCrawlQueue.equals(crawlID)) {
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

            int sentForQ =
                    sendURLsForQueue(
                            currentQueue,
                            currentCrawlQueue,
                            maxURLsPerQueue,
                            secsUntilRequestable,
                            now,
                            responseObserver);
            if (sentForQ > 0) {
                currentQueue.setLastProduced(now);
                totalSent += sentForQ;
                numQueuesSent++;
            }
        }

        LOG.info(
                "Sent {} from {} queue(s) in {} msec; tried {} queues",
                totalSent,
                numQueuesSent,
                (System.currentTimeMillis() - start),
                numQueuesTried);

        getURLs_urls_count.inc(totalSent);

        requestTimer.observeDuration();

        responseObserver.onCompleted();
    }

    protected abstract int sendURLsForQueue(
            QueueInterface queue,
            QueueWithinCrawl key,
            int maxURLsPerQueue,
            int secsUntilRequestable,
            long now,
            StreamObserver<URLInfo> responseObserver);

    @Override
    public void setLogLevel(LogLevelParams request, StreamObserver<Empty> responseObserver) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = loggerContext.getLogger(request.getPackage());
        logger.setLevel(Level.toLevel(request.getLevel().toString()));
        LOG.info("Log level for {} set to {}", request.getPackage(), request.getLevel().toString());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
