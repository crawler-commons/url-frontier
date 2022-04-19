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

import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage;
import crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.LoggerFactory;

public abstract class DistributedFrontierService extends AbstractFrontierService {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(DistributedFrontierService.class);

    /** Delete the queue based on the key in parameter */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long> responseObserver) {

        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());

        int sizeQueue = 0;

        if (!request.getLocal()) {
            for (String node : nodes) {
                if (node.equals(address)) continue;
                // call the delete endpoint in the target node
                // force to local so that remote node don't go recursive
                QueueWithinCrawlParams local =
                        QueueWithinCrawlParams.newBuilder(request).setLocal(true).build();
                ManagedChannel channel =
                        ManagedChannelBuilder.forTarget(node).usePlaintext().build();
                URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
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
            for (String node : nodes) {
                if (node.equals(address)) continue;
                // call the delete endpoint in the target node
                ManagedChannel channel =
                        ManagedChannelBuilder.forTarget(node).usePlaintext().build();
                URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
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

        if (request.getLocal()) {
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
        for (String node : nodes) {
            // call the delete endpoint in the target node
            ManagedChannel channel = ManagedChannelBuilder.forTarget(node).usePlaintext().build();
            URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
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
}
