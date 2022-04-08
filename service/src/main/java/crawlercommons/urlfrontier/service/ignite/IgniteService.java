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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.cache.Cache.Entry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.cache.query.TextQuery;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.slf4j.LoggerFactory;

public class IgniteService extends AbstractFrontierService implements Closeable {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(IgniteService.class);

    private static final String URLCacheNamePrefix = "urls_";

    private final Ignite ignite;

    private final ConcurrentHashMap<QueueWithinCrawl, QueueWithinCrawl> queuesBeingDeleted =
            new ConcurrentHashMap<>();

    // no explicit config
    public IgniteService() {
        this(new HashMap<String, String>());
    }

    public IgniteService(final Map<String, String> configuration) {

        IgniteConfiguration cfg = new IgniteConfiguration();

        // Classes of custom Java logic will be transferred over the wire from this app.
        cfg.setPeerClassLoadingEnabled(true);

        // client mode to make the frontier stateless is not supported yet
        // each instance holds some data

        // String clientMode = configuration.getOrDefault("ignite.client.mode",
        // "false");
        // cfg.setClientMode(Boolean.parseBoolean(clientMode));

        // "127.0.0.1:47500..47509"
        String igniteSeedAddress = configuration.get("ignite.seed.address");
        if (igniteSeedAddress != null) {
            // Setting up an IP Finder to ensure the client can locate the servers.
            TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
            ipFinder.setAddresses(Collections.singletonList(igniteSeedAddress));
            cfg.setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(ipFinder));
        }

        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        // specify where the data should be kept
        String path = configuration.get("ignite.path");
        if (path != null) {
            if (configuration.containsKey("ignite.purge")) {
                try {
                    Files.walk(Paths.get(path))
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException e) {
                    LOG.error("Couldn't delete path {}", path);
                }
            }
            storageCfg.setStoragePath(path);
        }
        // set persistence
        storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true);
        cfg.setDataStorageConfiguration(storageCfg);

        long start = System.currentTimeMillis();

        // Starting the node
        ignite = Ignition.start(cfg);

        ignite.cluster().state(ClusterState.ACTIVE);

        int backups = Integer.parseInt(configuration.getOrDefault("ignite.backups", "0"));

        // template for cache configurations
        CacheConfiguration cacheCfg = new CacheConfiguration("cacheTemplate");
        cacheCfg.setBackups(backups);
        cacheCfg.setCacheMode(CacheMode.PARTITIONED);
        ignite.addCacheConfiguration(cacheCfg);

        long end = System.currentTimeMillis();

        LOG.info("Ignite loaded in {} msec", end - start);

        LOG.info("Scanning tables to rebuild queues... (can take a long time)");

        queueScan(true);

        long end2 = System.currentTimeMillis();

        LOG.info("{} queues discovered in {} msec", queues.size(), (end2 - end));
    }

    private IgniteCache<Key, Payload> createOrGetCacheForCrawlID(String crawlID) {
        CacheConfiguration<Key, Payload> ccfg = new CacheConfiguration<>();
        ccfg.setName(URLCacheNamePrefix + crawlID);
        ccfg.setIndexedTypes(Key.class, Payload.class);
        return ignite.getOrCreateCache(ccfg);
    }

    @Override
    public void close() throws IOException {
        LOG.info("Closing Ignite");
        if (ignite != null) ignite.close();
    }

    /** Resurrects the queues from the tables * */
    private void queueScan(boolean localMode) {

        for (String cacheName : ignite.cacheNames()) {
            if (!cacheName.startsWith(URLCacheNamePrefix)) continue;

            IgniteCache<Key, Payload> cache = ignite.cache(cacheName);

            int urlsFound = 0;

            try (QueryCursor<Entry<Key, Payload>> cur =
                    cache.query(new ScanQuery<Key, Payload>().setLocal(localMode))) {
                for (Entry<Key, Payload> entry : cur) {
                    urlsFound++;
                    final QueueWithinCrawl qk =
                            QueueWithinCrawl.parseAndDeNormalise(entry.getKey().crawlQueueID);
                    QueueMetadata queueMD =
                            (QueueMetadata) queues.computeIfAbsent(qk, s -> new QueueMetadata());
                    // active if it has a scheduling value
                    boolean done = entry.getValue().nextFetchDate == 0;
                    if (done) {
                        queueMD.incrementCompleted();
                    } else {
                        queueMD.incrementActive();
                    }
                }
            }
            LOG.info(
                    "Found {} URLs for crawl : {}",
                    urlsFound,
                    cacheName.substring(URLCacheNamePrefix.length()));
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

                Payload oldpayload = null;

                final Key existenceKey = new Key(qk.toString(), url);

                Payload newpayload = new Payload(nextFetchDate, info.toByteArray());

                // is this URL already known?
                try {
                    oldpayload =
                            (Payload)
                                    createOrGetCacheForCrawlID(crawlID)
                                            .getAndPut(existenceKey, newpayload);
                } catch (Exception e) {
                    LOG.error("Ignite exception", e);
                    return;
                }

                // already known? ignore if discovered
                if (oldpayload != null && discovered) {
                    putURLs_alreadyknown_count.inc();
                    responseObserver.onNext(
                            crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
                                    .setValue(url)
                                    .build());
                    return;
                }

                // get the priority queue
                QueueMetadata queueMD =
                        (QueueMetadata) queues.computeIfAbsent(qk, s -> new QueueMetadata());

                // known - remove from queues
                // its key in the queues was stored in the default cf
                if (oldpayload != null) {
                    // remove from queue metadata
                    queueMD.removeFromProcessed(url);
                    queueMD.decrementActive();
                }

                // add the new item
                // unless it is an update and it's nextFetchDate is 0 == NEVER
                if (!discovered && nextFetchDate == 0) {
                    queueMD.incrementCompleted();
                    putURLs_completed_count.inc();
                } else {
                    // it is either brand new or already known
                    queueMD.incrementActive();
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

        Query<Entry<Key, Payload>> qry = new TextQuery<>(Payload.class, queueID.toString());
        // the content for the queues should only be local anyway
        qry.setLocal(true);

        try (QueryCursor<Entry<Key, Payload>> cursor =
                createOrGetCacheForCrawlID(queueID.getCrawlid()).query(qry)) {
            for (Entry<Key, Payload> entry : cursor) {
                if (alreadySent >= maxURLsPerQueue) break;
                // too early for it?
                long scheduled = entry.getValue().nextFetchDate;
                if (scheduled > now) {
                    // they should be sorted by date no need to go further
                    return alreadySent;
                }

                // check that the URL is not already being processed
                if (((QueueMetadata) queue).isHeld(entry.getKey().URL, now)) {
                    continue;
                }

                // this one is good to go
                try {
                    responseObserver.onNext(URLInfo.parseFrom(entry.getValue().payload));

                    // mark it as not processable for N secs
                    ((QueueMetadata) queue)
                            .holdUntil(entry.getKey().URL, now + secsUntilRequestable);

                    alreadySent++;
                } catch (InvalidProtocolBufferException e) {
                    LOG.error("Caught unlikely error ", e);
                }
            }
        }

        return alreadySent;
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

            // find the crawlIDs
            QueueWithinCrawl[] array = queues.keySet().toArray(new QueueWithinCrawl[0]);
            Arrays.sort(array);

            for (QueueWithinCrawl prefixed_queue : array) {
                boolean samePrefix = prefixed_queue.getCrawlid().equals(normalisedCrawlID);
                if (samePrefix) {
                    toDelete.add(prefixed_queue);
                }
            }

            ignite.destroyCache(URLCacheNamePrefix + normalisedCrawlID);

            for (QueueWithinCrawl quid : toDelete) {
                if (queuesBeingDeleted.contains(quid)) {
                    continue;
                } else {
                    queuesBeingDeleted.put(quid, quid);
                }

                QueueInterface q = queues.remove(quid);
                total += q.countActive();
                total += q.getCountCompleted();

                queuesBeingDeleted.remove(quid);
            }
        }
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder()
                        .setValue(total)
                        .build());
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
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer> responseObserver) {

        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());

        int sizeQueue = 0;

        // if the queue is unknown
        if (!queues.containsKey(qc)) {
            responseObserver.onNext(
                    crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder()
                            .setValue(sizeQueue)
                            .build());
            responseObserver.onCompleted();
            return;
        }

        // is this queue already being deleted?
        // no need to do it again
        if (queuesBeingDeleted.contains(qc)) {
            responseObserver.onNext(
                    crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder()
                            .setValue(sizeQueue)
                            .build());
            responseObserver.onCompleted();
            return;
        }

        queuesBeingDeleted.put(qc, qc);

        IgniteCache<Key, Payload> URLCache = createOrGetCacheForCrawlID(request.getCrawlID());

        Query<Entry<Key, Payload>> qry = new TextQuery<>(Payload.class, qc.toString());
        // the content for the queues should only be local anyway
        qry.setLocal(true);

        try (QueryCursor<Entry<Key, Payload>> cur = URLCache.query(qry)) {
            for (Entry<Key, Payload> entry : cur) {
                URLCache.remove(entry.getKey());
            }
        }

        QueueInterface q = queues.remove(qc);
        sizeQueue += q.countActive();
        sizeQueue += q.getCountCompleted();

        queuesBeingDeleted.remove(qc);

        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder()
                        .setValue(sizeQueue)
                        .build());
        responseObserver.onCompleted();
    }
}
