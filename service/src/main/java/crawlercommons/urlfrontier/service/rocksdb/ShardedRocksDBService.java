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

import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.cluster.DistributedFrontierService;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Map;

/**
 * Started with a pre-set and definitive list of nodes, forwards incoming data to each node based on
 * a hash of the queue
 */
public class ShardedRocksDBService extends DistributedFrontierService {

    private RocksDBService instance;

    public ShardedRocksDBService(final Map<String, String> configuration) {
        instance = new RocksDBService(configuration);
        // take coordinates of the nodes + able to identify itself in the list
        String snodes = configuration.get("ignite.nodes");
        if (snodes == null) {
            throw new RuntimeException("ShardedRocksDBService requires ignite.nodes to be set");
        }
        // comma separated
        for (String n : snodes.split(",")) {
            nodes.add(n.trim());
        }
        if (nodes.size() > 1) {
            clusterMode = true;
        }
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
            StreamObserver<URLInfo> responseObserver) {
        return instance.sendURLsForQueue(
                queue, key, maxURLsPerQueue, secsUntilRequestable, now, responseObserver);
    }

    @Override
    public void close() throws IOException {
        super.close();
        instance.close();
    }

    @Override
    protected String putURLItem(URLItem item) {
        return instance.putURLItem(item);
    }
}
