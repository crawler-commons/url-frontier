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
package crawlercommons.urlfrontier.service.rocksdb;

import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.SynchronizedStreamObserver;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.BloomFilter;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Statistics;
import org.rocksdb.StatsLevel;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;
import org.slf4j.LoggerFactory;

public class RocksDBService extends AbstractFrontierService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RocksDBService.class);

    private static final DecimalFormat DF = new DecimalFormat("0000000000");

    static {
        RocksDB.loadLibrary();
    }

    private RocksDB rocksDB;

    // a list which will hold the handles for the column families once the db is
    // opened
    private final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

    private Statistics statistics;

    // no explicit config
    public RocksDBService(String host, int port) {
        this(new HashMap<String, String>(), host, port);
    }

    private final ConcurrentHashMap<QueueWithinCrawl, QueueWithinCrawl> queuesBeingDeleted =
            new ConcurrentHashMap<>();

    public RocksDBService(final Map<String, String> configuration, String host, int port) {

        // configure the number of threads for puts
        super(configuration, host, port);

        // where to store it?
        String path = configuration.getOrDefault("rocksdb.path", "./rocksdb");

        LOG.info("RocksDB data stored in {} ", path);

        if (configuration.containsKey("rocksdb.purge")) {
            createOrCleanDirectory(path);
            LOG.info("Purged storage path {}", path);
        }

        if (configuration.containsKey("rocksdb.stats")) {
            statistics = new Statistics();
            statistics.setStatsLevel(StatsLevel.ALL);
        }

        boolean checkOnRecovery = configuration.containsKey("rocksdb.recovery.check");

        boolean bloomFilters = configuration.containsKey("rocksdb.bloom.filters");

        try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions()) {

            String sMaxBytesForLevelBase = configuration.get("rocksdb.max_bytes_for_level_base");
            if (sMaxBytesForLevelBase != null) {
                cfOpts.setMaxBytesForLevelBase(Long.parseLong(sMaxBytesForLevelBase));
            }

            cfOpts.optimizeUniversalStyleCompaction();

            // list of column family descriptors, first entry must always be default column
            // family
            final List<ColumnFamilyDescriptor> cfDescriptors =
                    Arrays.asList(
                            new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
                            new ColumnFamilyDescriptor("queues".getBytes(), cfOpts),
                            new ColumnFamilyDescriptor("queueInfos".getBytes(), cfOpts));

            long start = System.currentTimeMillis();

            if (bloomFilters) {
                LOG.info("Configuring Bloom filters");
                cfOpts.setTableFormatConfig(
                        new BlockBasedTableConfig().setFilterPolicy(new BloomFilter(10, false)));
            }

            try (final DBOptions options = new DBOptions()) {
                options.setCreateIfMissing(true).setCreateMissingColumnFamilies(true);

                String smaxBackgroundJobs = configuration.get("rocksdb.max_background_jobs");
                if (smaxBackgroundJobs != null) {
                    options.setMaxBackgroundJobs(Integer.parseInt(smaxBackgroundJobs));
                }

                // Options.max_subcompactions: 1
                String smax_subcompactions = configuration.get("rocksdb.max_subcompactions");
                if (smax_subcompactions != null) {
                    options.setMaxSubcompactions(Integer.parseInt(smax_subcompactions));
                }

                if (statistics != null) {
                    LOG.info("Allowing stats from RocksDB to be displayed when GetStats is called");
                    options.setStatistics(statistics);
                }

                rocksDB = RocksDB.open(options, path, cfDescriptors, columnFamilyHandleList);
            } catch (RocksDBException e) {
                LOG.error("RocksDB exception ", e);
                throw new RuntimeException(e);
            }

            long end = System.currentTimeMillis();

            LOG.info("RocksDB loaded in {} msec", end - start);

            // full re-check
            if (checkOnRecovery) {
                LOG.info("Scanning tables to rebuild queues... (can take a long time)");
                recoveryQscan(true);
            } else {
                recovery();
            }

            long end2 = System.currentTimeMillis();

            LOG.info("{} queues discovered in {} msec", getQueues().size(), (end2 - end));
        }
    }

    private void recovery() {
        // if a table containing the queues info exists use it,
        // otherwise just rebuild from the content of the tables
        int read = 0;
        try {
            read = readQueueInfos();
        } catch (RocksDBException e) {
            LOG.error("readQueueInfos", e);
        }
        // nothing found? rebuild
        if (read == 0) {
            recoveryQscan(false);
        }
    }

    /** Resurrects the queues from the URL tables and optionally does sanity checks * */
    private void recoveryQscan(boolean check) {

        LOG.info("Recovering queues from existing RocksDB");

        if (check) {
            try (final RocksIterator rocksIterator =
                    rocksDB.newIterator(columnFamilyHandleList.get(1))) {
                for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                    final String currentKey =
                            new String(rocksIterator.key(), StandardCharsets.UTF_8);
                    final QueueWithinCrawl qk = QueueWithinCrawl.parseAndDeNormalise(currentKey);
                    QueueMetadata queueMD =
                            (QueueMetadata)
                                    getQueues().computeIfAbsent(qk, s -> new QueueMetadata());
                    queueMD.incrementActive();
                }
            }
            LOG.info("Found {} queues from scheduled table", getQueues().size());
        }

        QueueWithinCrawl previousQueueID = null;
        long numScheduled = 0;

        // now get the counts of URLs already finished
        try (final RocksIterator rocksIterator =
                rocksDB.newIterator(columnFamilyHandleList.get(0))) {
            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                final String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);
                final QueueWithinCrawl Qkey = QueueWithinCrawl.parseAndDeNormalise(currentKey);

                // changed ID? check that the previous one had the correct values
                if (previousQueueID == null) {
                    previousQueueID = Qkey;
                } else if (check && !previousQueueID.equals(Qkey)) {
                    int activeinQueues = getQueues().get(previousQueueID).countActive();
                    if (activeinQueues != numScheduled) {
                        LOG.error(
                                "Incorrect number of active URLs for queue: {}. {} vs {}",
                                previousQueueID,
                                activeinQueues,
                                numScheduled);
                        throw new RuntimeException(
                                "Incorrect number of active URLs for queue: " + previousQueueID);
                    }
                    previousQueueID = Qkey;
                    numScheduled = 0;
                }

                // queue might not exist if it had nothing scheduled for it
                // i.e. all done
                QueueMetadata queueMD =
                        (QueueMetadata) getQueues().computeIfAbsent(Qkey, s -> new QueueMetadata());

                // check the value - if it is an empty byte array it means that the URL has been
                // processed and is not scheduled
                // otherwise it is scheduled
                boolean done = rocksIterator.value().length == 0;
                if (done) {
                    queueMD.incrementCompleted();
                } else {
                    // if no checks have been done increment active
                    if (!check) {
                        queueMD.incrementActive();
                    }
                    // double check the number of scheduled later on
                    numScheduled++;
                }
            }
        }
        // check the last key
        if (check
                && previousQueueID != null
                && getQueues().get(previousQueueID).countActive() != numScheduled) {
            throw new RuntimeException(
                    "Incorrect number of active URLs for queue " + previousQueueID);
        }
    }

    @Override
    protected int sendURLsForQueue(
            QueueInterface queue,
            QueueWithinCrawl queueID,
            int maxURLsPerQueue,
            int secsUntilRequestable,
            long now,
            SynchronizedStreamObserver<URLInfo> responseObserver) {

        // stop sending if we are closing
        if (isClosing()) {
            return 0;
        }

        int alreadySent = 0;
        final byte[] prefixKey = (queueID.toString() + "_").getBytes(StandardCharsets.UTF_8);
        // scan the scheduling table
        try (final RocksIterator rocksIterator =
                rocksDB.newIterator(columnFamilyHandleList.get(1))) {
            for (rocksIterator.seek(prefixKey);
                    rocksIterator.isValid() && alreadySent < maxURLsPerQueue;
                    rocksIterator.next()) {

                final String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);

                // don't want to split the whole string _ as the URL part is left as is
                final int pos = currentKey.indexOf('_');
                final int pos2 = currentKey.indexOf('_', pos + 1);
                final int pos3 = currentKey.indexOf('_', pos2 + 1);

                final String crawlPart = currentKey.substring(0, pos);
                final String queuePart = currentKey.substring(pos + 1, pos2);
                final String urlPart = currentKey.substring(pos3 + 1);

                // not for this queue anymore?
                if (!queueID.equals(crawlPart, queuePart)) {
                    return alreadySent;
                }

                // too early for it?
                long scheduled = Long.parseLong(currentKey.substring(pos2 + 1, pos3));
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
                    // check that we haven't already reached the number of queues
                    if (alreadySent == 0 && !responseObserver.tryTakingToken()) {
                        return 0;
                    }

                    responseObserver.onNext(URLInfo.parseFrom(rocksIterator.value()));

                    // mark it as not processable for N secs
                    ((QueueMetadata) queue).holdUntil(urlPart, now + secsUntilRequestable);

                    alreadySent++;
                } catch (Throwable e) {
                    LOG.error("Caught unlikely error ", e);
                }
            }
        }

        return alreadySent;
    }

    @Override
    protected Status putURLItem(final URLItem value) {

        if (isClosing()) {
            return Status.FAIL;
        }

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
        final String url = info.getUrl();
        final String crawlID = CrawlID.normaliseCrawlID(info.getCrawlID());

        // has a queue key been defined? if not use the hostname
        if (Qkey.equals("")) {
            LOG.debug("key missing for {}", url);
            Qkey = provideMissingKey(url);
            if (Qkey == null) {
                LOG.error("Malformed URL {}", url);
                return Status.SKIPPED;
            }
            // make a new info object ready to return
            info = URLInfo.newBuilder(info).setKey(Qkey).setCrawlID(crawlID).build();
        }

        LOG.debug("putURLItem -> {} with key {} in crawl {}", url, Qkey, crawlID);

        // check that the key is not too long
        if (Qkey.length() > 255) {
            LOG.error("Key too long: {}", Qkey);
            return Status.SKIPPED;
        }

        final QueueWithinCrawl qk = QueueWithinCrawl.get(Qkey, crawlID);

        // ignore this url if the queue is being deleted
        if (queuesBeingDeleted.containsKey(qk)) {
            LOG.info("Not adding {} as its queue {} is being deleted", url, Qkey);
            return Status.SKIPPED;
        }

        // make it intern so that all threads accessing this method
        // share the same instance of the String, this way we can synchronize
        // on it and make sure that 2 threads working on the same URL won't
        // both be considered non-existant
        final String existenceKeyString = (qk.toString() + "_" + url).intern();
        final byte[] existenceKey = existenceKeyString.getBytes(StandardCharsets.UTF_8);

        synchronized (existenceKeyString) {

            // is this URL already known?
            try (WriteBatch writeBatch = new WriteBatch();
                    WriteOptions writeOps = new WriteOptions()) {

                if (isClosing()) {
                    return Status.FAIL;
                }
                byte[] schedulingKey = rocksDB.get(existenceKey);

                // already known? ignore if discovered
                if (schedulingKey != null && discovered) {
                    putURLs_alreadyknown_count.inc();
                    return Status.SKIPPED;
                }

                // get the priority queue or create one
                QueueMetadata queueMD =
                        (QueueMetadata) getQueues().computeIfAbsent(qk, s -> new QueueMetadata());

                // known - remove from queues
                // its key in the queues was stored in the default cf
                if (schedulingKey != null) {
                    if (isClosing()) {
                        return Status.FAIL;
                    }
                    writeBatch.delete(columnFamilyHandleList.get(1), schedulingKey);
                    // remove from queue metadata
                    queueMD.removeFromProcessed(url);
                    queueMD.decrementActive();
                }

                // add the new item
                // unless it is an update and it's nextFetchDate is 0 == NEVER
                if (!discovered && nextFetchDate == 0) {
                    // does not need scheduling
                    // remove any scheduling key from its value
                    schedulingKey = new byte[] {};
                    queueMD.incrementCompleted();
                    putURLs_completed_count.inc();
                } else {
                    // it is either brand new or already known
                    // create a scheduling key for it
                    schedulingKey =
                            (qk.toString() + "_" + DF.format(nextFetchDate) + "_" + url)
                                    .getBytes(StandardCharsets.UTF_8);
                    // add to the scheduling
                    if (isClosing()) {
                        return Status.FAIL;
                    }
                    writeBatch.put(
                            columnFamilyHandleList.get(1), schedulingKey, info.toByteArray());
                    queueMD.incrementActive();
                }

                if (isClosing()) {
                    return Status.FAIL;
                }

                // update the link to its queue
                writeBatch.put(columnFamilyHandleList.get(0), existenceKey, schedulingKey);

                // batch the updates - this way the scheduling and main tables will always be in
                // sync
                rocksDB.write(writeOps, writeBatch);

            } catch (RocksDBException e) {
                LOG.error("RocksDB exception", e);
                return Status.FAIL;
            }
        }

        return Status.OK;
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
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long> responseObserver) {
        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());
        int sizeQueue = deleteLocalQueue(qc);
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder()
                        .setValue(sizeQueue)
                        .build());
        responseObserver.onCompleted();
    }

    protected int deleteLocalQueue(QueueWithinCrawl qc) {
        int sizeQueue = 0;

        if (isClosing()) {
            return 0;
        }

        // don't have that queue?
        if (!getQueues().containsKey(qc)) {
            return sizeQueue;
        }

        // is this queue already being deleted?
        // no need to do it again
        if (queuesBeingDeleted.contains(qc)) {
            return sizeQueue;
        }

        queuesBeingDeleted.put(qc, qc);

        String prefixed_queue = qc.toString() + "_";

        // find the next key by alphabetical order, taking the separator into account
        QueueWithinCrawl[] array = getQueues().keySet().toArray(new QueueWithinCrawl[0]);
        String[] prefixed_queues = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            prefixed_queues[i] = array[i] + "_";
        }
        Arrays.sort(prefixed_queues);
        byte[] startKey = null;
        byte[] endKey = null;
        for (String p_queue : prefixed_queues) {
            if (startKey != null) {
                endKey = p_queue.getBytes(StandardCharsets.UTF_8);
                break;
            } else if (prefixed_queue.equals(p_queue)) {
                startKey = prefixed_queue.getBytes(StandardCharsets.UTF_8);
            }
        }

        try {
            deleteRanges(startKey, endKey);
        } catch (RocksDBException e) {
            LOG.error(
                    "Exception caught when deleting ranges - {} - {}",
                    new String(startKey),
                    new String(endKey),
                    e);
        }

        QueueInterface q = getQueues().remove(qc);
        sizeQueue += q.countActive();
        sizeQueue += q.getCountCompleted();

        queuesBeingDeleted.remove(qc);
        return sizeQueue;
    }

    @Override
    public void getStats(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<Stats> responseObserver) {
        if (statistics != null) {
            LOG.info("RockdSB stats: {}", statistics);
        }
        super.getStats(request, responseObserver);
    }

    @Override
    public void close() throws IOException {

        LOG.info("Closing RocksDB");

        super.close();

        // persisting the counts for the queues
        try {
            writeQueueInfos();
        } catch (Exception e) {
            LOG.error("writeQueueInfos ", e);
            rocksDB.destroyColumnFamilyHandle(columnFamilyHandleList.get(2));
        }

        for (final ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleList) {
            columnFamilyHandle.close();
        }

        if (statistics != null) {
            statistics.close();
        }

        if (rocksDB != null) {
            try {
                rocksDB.syncWal();
                rocksDB.closeE();
            } catch (Exception e) {
                LOG.error("Closing ", e);
            }
        }
    }

    /**
     * When shutting down an instance - write the stats about the queues into a table for faster
     * reloading later
     *
     * @throws RocksDBException
     */
    private void writeQueueInfos() throws RocksDBException {
        long start = System.currentTimeMillis();
        int queuesWritten = 0;
        if (!getQueues().isEmpty()) {
            ByteBuffer bb = ByteBuffer.allocate(8);
            synchronized (getQueues()) {
                for (Entry<QueueWithinCrawl, QueueInterface> entry : getQueues().entrySet()) {
                    queuesWritten++;
                    int active = entry.getValue().countActive();
                    int completed = entry.getValue().getCountCompleted();
                    bb.putInt(active);
                    bb.putInt(completed);
                    rocksDB.put(
                            columnFamilyHandleList.get(2),
                            entry.getKey().toString().getBytes(),
                            bb.array());
                    bb.clear();
                }
            }
        }
        long end = System.currentTimeMillis();
        LOG.info(
                "writeQueueInfos stored stats for {} queues in {} msec",
                queuesWritten,
                (end - start));
    }

    private int readQueueInfos() throws RocksDBException {
        long start = System.currentTimeMillis();
        int queuesRead = 0;
        byte[] firstKey = null;
        byte[] lastKey = null;
        ByteBuffer bb = ByteBuffer.allocate(8);
        try (final RocksIterator rocksIterator =
                rocksDB.newIterator(columnFamilyHandleList.get(2))) {
            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
                if (firstKey == null) firstKey = rocksIterator.key();
                lastKey = rocksIterator.key();
                queuesRead++;
                final String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);
                final QueueWithinCrawl qk = QueueWithinCrawl.parseAndDeNormalise(currentKey);
                QueueMetadata queueMD =
                        (QueueMetadata) getQueues().computeIfAbsent(qk, s -> new QueueMetadata());
                rocksIterator.value(bb);
                int active = bb.getInt();
                int completed = bb.getInt();
                bb.clear();
                queueMD.setActiveCount(active);
                queueMD.setCompletedCount(completed);
            }
        }

        if (queuesRead != 0) {
            // empty the table so that we don't read it again
            // if there has been a nasty crash
            rocksDB.deleteRange(columnFamilyHandleList.get(2), firstKey, lastKey);
        }

        long end = System.currentTimeMillis();
        LOG.info("readQueueInfos read stats for {} queues in {} msec", queuesRead, (end - start));

        return queuesRead;
    }

    @Override
    public void deleteCrawl(
            crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage crawlID,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
                    responseObserver) {
        long total = deleteLocalCrawl(crawlID.getValue());
        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder().setValue(total).build());
        responseObserver.onCompleted();
    }

    protected long deleteLocalCrawl(String crawlID) {
        long total = 0;

        if (isClosing()) {
            return 0;
        }

        final String normalisedCrawlID = CrawlID.normaliseCrawlID(crawlID);

        final Set<QueueWithinCrawl> toDelete = new HashSet<>();

        synchronized (getQueues()) {

            // find the crawlIDs
            QueueWithinCrawl[] array = getQueues().keySet().toArray(new QueueWithinCrawl[0]);
            Arrays.sort(array);

            byte[] startKey = null;
            byte[] endKey = null;
            for (QueueWithinCrawl prefixed_queue : array) {
                boolean samePrefix = prefixed_queue.getCrawlid().equals(normalisedCrawlID);
                if (samePrefix) {
                    if (startKey == null) {
                        startKey =
                                (prefixed_queue.getCrawlid().replaceAll("_", "%5F") + "_")
                                        .getBytes(StandardCharsets.UTF_8);
                    }
                    toDelete.add(prefixed_queue);
                } else if (startKey != null) {
                    endKey =
                            (prefixed_queue.getCrawlid().replaceAll("_", "%5F") + "_")
                                    .getBytes(StandardCharsets.UTF_8);
                    break;
                }
            }

            // no queues found - nothing to delete
            if (startKey == null) {
                return 0;
            }

            try {
                deleteRanges(startKey, endKey);
            } catch (RocksDBException e) {
                LOG.error(
                        "Exception caught when deleting ranges - {} - {}",
                        new String(startKey),
                        new String(endKey));
            }

            for (QueueWithinCrawl quid : toDelete) {
                if (queuesBeingDeleted.contains(quid)) {
                    continue;
                } else {
                    queuesBeingDeleted.put(quid, quid);
                }

                QueueInterface q = getQueues().remove(quid);
                total += q.countActive();
                total += q.getCountCompleted();

                queuesBeingDeleted.remove(quid);
            }
        }
        return total;
    }

    private void deleteRanges(final byte[] prefix, byte[] endKey) throws RocksDBException {

        if (isClosing()) {
            return;
        }

        // if endKey is null it means that there is no other crawlID after this one
        boolean includeEndKey = false;

        if (endKey == null) {
            try (RocksIterator iter = rocksDB.newIterator(columnFamilyHandleList.get(0))) {
                iter.seekToLast();
                if (iter.isValid()) {
                    // this is the last known URL
                    endKey = iter.key();
                    includeEndKey = true;
                }
            }
        }

        // no end key found?
        if (endKey == null) {
            throw new RuntimeException("No endkey found");
        }

        // delete the ranges in the queues table as well as the URLs already
        // processed
        rocksDB.deleteRange(columnFamilyHandleList.get(1), prefix, endKey);
        rocksDB.deleteRange(columnFamilyHandleList.get(0), prefix, endKey);

        if (includeEndKey) {
            rocksDB.deleteRange(columnFamilyHandleList.get(1), endKey, endKey);
            rocksDB.delete(columnFamilyHandleList.get(0), endKey);
        }
    }
}
