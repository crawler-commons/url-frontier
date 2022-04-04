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
package crawlercommons.urlfrontier.service.ignite;

import com.google.protobuf.InvalidProtocolBufferException;
import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.rocksdb.QueueMetadata;
import io.grpc.stub.StreamObserver;
import java.io.Closeable;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.cache.Cache.Entry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.slf4j.LoggerFactory;

public class IgniteService extends AbstractFrontierService implements Closeable {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(IgniteService.class);

    private static final DecimalFormat DF = new DecimalFormat("0000000000");

    private final Ignite ignite;

    private final ConcurrentHashMap<QueueWithinCrawl, QueueWithinCrawl> queuesBeingDeleted =
            new ConcurrentHashMap<>();

    private final IgniteCache<String, String> urls_cache;
    private final IgniteCache<String, byte[]> scheduling_cache;

    // no explicit config
    public IgniteService() {
        this(new HashMap<String, String>());
    }

    public IgniteService(final Map<String, String> configuration) {

        String igniteSeedAddress =
                configuration.getOrDefault("ignite.server", "127.0.0.1:47500..47509");

        IgniteConfiguration cfg = new IgniteConfiguration();

        // The node will be started as a client node.
        cfg.setClientMode(true);

        // Classes of custom Java logic will be transferred over the wire from this app.
        // cfg.setPeerClassLoadingEnabled(true);

        // Setting up an IP Finder to ensure the client can locate the servers.
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(Collections.singletonList(igniteSeedAddress));
        cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(ipFinder));

        long start = System.currentTimeMillis();

        // Starting the node
        ignite = Ignition.start(cfg);

        urls_cache = ignite.getOrCreateCache("urls");
        scheduling_cache = ignite.getOrCreateCache("schedule");

        long end = System.currentTimeMillis();

        LOG.info("Ignite loaded in {} msec", end - start);

        LOG.info("Scanning tables to rebuild queues... (can take a long time)");

        recoveryQscan();

        long end2 = System.currentTimeMillis();

        LOG.info("{} queues discovered in {} msec", queues.size(), (end2 - end));
    }

    @Override
    public void close() throws IOException {
        if (ignite != null) ignite.close();
    }

    /** Resurrects the queues from the tables and does sanity checks * */
    private void recoveryQscan() {

        LOG.info("Recovering queues from existing Ignite");

        Query<Entry<String, String>> qry =
                new ScanQuery<String, String>(
                        (i, p) -> {
                            return true;
                        });

        try (QueryCursor<Entry<String, String>> cur = urls_cache.query(qry)) {
            for (Entry<String, String> entry : cur) {
                final QueueWithinCrawl qk = QueueWithinCrawl.parseAndDeNormalise(entry.getKey());
                QueueMetadata queueMD =
                        (QueueMetadata) queues.computeIfAbsent(qk, s -> new QueueMetadata());
                // active if it has a scheduling value
                boolean done = entry.getValue().length() == 0;
                if (done) {
                    queueMD.incrementCompleted();
                } else {
                    queueMD.incrementActive();
                }
            }
        }
    }

    @Override
    public StreamObserver<URLItem> putURLs(
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {

        putURLs_calls.inc();

        return new StreamObserver<URLItem>() {

            @Override
            public void onNext(URLItem value) {

                long nextFetchDate;
                boolean discovered = true;
                URLInfo info;

                putURLs_urls_count.inc();

                if (value.hasDiscovered()) {
                    putURLs_discovered_count.labels("true").inc();
                    info = value.getDiscovered().getInfo();
                    nextFetchDate = Instant.now().getEpochSecond();
                } else {
                    putURLs_discovered_count.labels("false").inc();
                    KnownURLItem known = value.getKnown();
                    info = known.getInfo();
                    nextFetchDate = known.getRefetchableFromDate();
                    discovered = Boolean.FALSE;
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

                // check that the key is not too long
                if (Qkey.length() > 255) {
                    LOG.error("Key too long: {}", Qkey);
                    responseObserver.onNext(
                            crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                    .setValue(url)
                                    .build());
                    return;
                }

                QueueWithinCrawl qk = QueueWithinCrawl.get(Qkey, crawlID);

                // ignore this url if the queue is being deleted
                if (queuesBeingDeleted.containsKey(qk)) {
                    LOG.info("Not adding {} as its queue {} is being deleted", url, Qkey);
                    responseObserver.onNext(
                            crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                    .setValue(url)
                                    .build());
                    return;
                }

                String schedulingKey = null;

                final String existenceKey = (normalise(qk) + "_" + url);

                // is this URL already known?
                try {
                    schedulingKey = urls_cache.get(existenceKey);
                } catch (Exception e) {
                    LOG.error("Ignite exception", e);
                    return;
                }

                // already known? ignore if discovered
                if (schedulingKey != null && discovered) {
                    putURLs_alreadyknown_count.inc();
                    responseObserver.onNext(
                            crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                    .setValue(url)
                                    .build());
                    return;
                }

                // get the priority queue or create one
                QueueMetadata queueMD =
                        (QueueMetadata) queues.computeIfAbsent(qk, s -> new QueueMetadata());
                try {
                    // known - remove from queues
                    // its key in the queues was stored in the default cf
                    if (schedulingKey != null) {
                        scheduling_cache.remove(schedulingKey);
                        // remove from queue metadata
                        queueMD.removeFromProcessed(url);
                        queueMD.decrementActive();
                    }

                    // add the new item
                    // unless it is an update and it's nextFetchDate is 0 == NEVER
                    if (!discovered && nextFetchDate == 0) {
                        // does not need scheduling
                        // remove any scheduling key from its value
                        schedulingKey = "";
                        queueMD.incrementCompleted();
                        putURLs_completed_count.inc();
                    } else {
                        // it is either brand new or already known
                        // create a scheduling key for it
                        schedulingKey =
                                (normalise(qk) + "_" + DF.format(nextFetchDate) + "_" + url);
                        // add to the scheduling
                        scheduling_cache.put(schedulingKey, info.toByteArray());
                        queueMD.incrementActive();
                    }
                    // update the link to its queue
                    urls_cache.put(existenceKey, schedulingKey);
                } catch (Exception e) {
                    LOG.error("Ignite exception", e);
                }

                responseObserver.onNext(
                        crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                .setValue(url)
                                .build());
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

    @Override
    protected int sendURLsForQueue(
            QueueInterface queue,
            QueueWithinCrawl queueID,
            int maxURLsPerQueue,
            int secsUntilRequestable,
            long now,
            StreamObserver<URLInfo> responseObserver) {

        int alreadySent = 0;
        final String prefixKey = (normalise(queueID) + "_");

        // TODO optimise by identifying the partition where the data can be found
        Query<Entry<String, byte[]>> qry =
                new ScanQuery<String, byte[]>((i, p) -> i.startsWith(prefixKey));

        try (QueryCursor<Entry<String, byte[]>> cur = scheduling_cache.query(qry)) {
            for (Entry<String, byte[]> entry : cur) {
                if (alreadySent >= maxURLsPerQueue) break;
                // don't want to split the whole string _ as the URL part is left as is
                final int pos = entry.getKey().indexOf('_');
                final int pos2 = entry.getKey().indexOf('_', pos + 1);
                final int pos3 = entry.getKey().indexOf('_', pos2 + 1);

                final String crawlPart = entry.getKey().substring(0, pos);
                final String queuePart = entry.getKey().substring(pos + 1, pos2);
                final String urlPart = entry.getKey().substring(pos3 + 1);

                // not for this queue anymore?
                if (!queueID.equals(crawlPart, queuePart)) {
                    return alreadySent;
                }

                // too early for it?
                long scheduled = Long.parseLong(entry.getKey().substring(pos2 + 1, pos3));
                if (scheduled > now) {
                    // they are sorted by date no need to go further
                    return alreadySent;
                }

                // check that the URL is not already being processed
                if (((QueueMetadata) queue).isHeld(urlPart, now)) {
                    continue;
                }

                // this one is good to go
                try {
                    responseObserver.onNext(URLInfo.parseFrom(entry.getValue()));

                    // mark it as not processable for N secs
                    ((QueueMetadata) queue).holdUntil(urlPart, now + secsUntilRequestable);

                    alreadySent++;
                } catch (InvalidProtocolBufferException e) {
                    LOG.error("Caught unlikely error ", e);
                }
            }
        }

        return alreadySent;
    }

    /** underscores being used as separator for the keys */
    public static final String normalise(QueueWithinCrawl qwc) {
        return qwc.getCrawlid().replaceAll("_", "%5F")
                + "_"
                + qwc.getQueue().replaceAll("_", "%5F");
    }
}
