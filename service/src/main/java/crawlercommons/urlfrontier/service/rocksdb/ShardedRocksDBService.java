// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.CloseableIterator;
import crawlercommons.urlfrontier.service.ConcurrentInsertionOrderMap;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.SynchronizedStreamObserver;
import crawlercommons.urlfrontier.service.cluster.DistributedFrontierService;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Started with a pre-set and definitive list of nodes, forwards incoming data to each node based on
 * a hash of the queue
 */
public class ShardedRocksDBService extends DistributedFrontierService {

    private final RocksDBService instance;

    public ShardedRocksDBService(final Map<String, String> configuration, String host, int port) {
        super(host, port);

        instance = new RocksDBService(configuration, host, port);
        // take coordinates of the nodes + able to identify itself in the list
        final String snodes = configuration.get("nodes");
        if (snodes == null) {
            throw new RuntimeException(
                    "ShardedRocksDBService requires configuration 'nodes' to be set");
        }
        // comma separated
        final List<String> lnodes = new ArrayList<>();
        for (String n : snodes.split(",")) {
            lnodes.add(n.trim());
        }
        if (lnodes.size() > 1) {
            clusterMode = true;
        }
        setNodes(lnodes);
    }

    @Override
    protected int deleteLocalQueue(QueueWithinCrawl qc) {
        return instance.deleteLocalQueue(qc);
    }

    @Override
    protected long deleteLocalCrawl(String crawlID) {
        return instance.deleteLocalCrawl(crawlID);
    }

    @Override
    protected int sendURLsForQueue(
            QueueInterface queue,
            QueueWithinCrawl key,
            int maxURLsPerQueue,
            int secsUntilRequestable,
            long now,
            SynchronizedStreamObserver<URLInfo> responseObserver) {
        return instance.sendURLsForQueue(
                queue, key, maxURLsPerQueue, secsUntilRequestable, now, responseObserver);
    }

    @Override
    public void close() throws IOException {
        super.close();
        instance.close();
    }

    @Override
    protected Status putURLItem(URLItem item) {
        return instance.putURLItem(item);
    }

    @Override
    public ConcurrentInsertionOrderMap<QueueWithinCrawl, QueueInterface> getQueues() {
        return instance.getQueues();
    }

    @Override
    // TODO Implementation of getURLStatus for ShardedRocksDB
    public void getURLStatus(URLStatusRequest request, StreamObserver<URLItem> responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    // TODO Implementation of listURLs for ShardedRocksDB
    public void listURLs(ListUrlParams request, StreamObserver<URLItem> responseObserver) {
        responseObserver.onError(io.grpc.Status.UNIMPLEMENTED.asException());
    }

    @Override
    // TODO Implementation of urlIterator for ShardedRocksDB
    protected CloseableIterator<URLItem> urlIterator(
            Entry<QueueWithinCrawl, QueueInterface> qentry, long start, long max) {
        throw new UnsupportedOperationException(
                "Feature not implemented for ShardedRocksDB backend");
    }
}
