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
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public abstract class DistributedFrontierService extends AbstractFrontierService {

    /** Delete the queue based on the key in parameter */
    @Override
    public void deleteQueue(
            crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams request,
            StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long> responseObserver) {

        final QueueWithinCrawl qc = QueueWithinCrawl.get(request.getKey(), request.getCrawlID());

        int sizeQueue = 0;

        // if the queue is unknown - distribute the call to all the other nodes in the
        // cluster
        if (!queues.containsKey(qc)) {
            for (String node : nodes) {
                if (nodes.equals(address)) continue;
                // call the delete endpoint in the target node
                ManagedChannel channel =
                        ManagedChannelBuilder.forTarget(node).usePlaintext().build();
                URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
                crawlercommons.urlfrontier.Urlfrontier.Long total =
                        blockingFrontier.deleteQueue(request);
                sizeQueue += total.getValue();
            }

            responseObserver.onNext(
                    crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder()
                            .setValue(sizeQueue)
                            .build());
            responseObserver.onCompleted();
            return;
        } else {
            // delete the queue held by this node
            sizeQueue = deleteLocalQueue(qc);
        }

        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder()
                        .setValue(sizeQueue)
                        .build());
        responseObserver.onCompleted();
    }

    protected abstract int deleteLocalQueue(final QueueWithinCrawl qc);

    @Override
    public void deleteCrawl(
            crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage crawlID,
            io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Long>
                    responseObserver) {
        long total = 0;
        final String normalisedCrawlID = CrawlID.normaliseCrawlID(crawlID.getValue());

        for (String node : nodes) {
            if (nodes.equals(address)) continue;
            // call the delete endpoint in the target node
            ManagedChannel channel = ManagedChannelBuilder.forTarget(node).usePlaintext().build();
            URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
            crawlercommons.urlfrontier.Urlfrontier.Long local =
                    blockingFrontier.deleteCrawl(crawlID);
            total += local.getValue();
        }

        // delete on the current node
        total += deleteLocalCrawl(normalisedCrawlID);

        responseObserver.onNext(
                crawlercommons.urlfrontier.Urlfrontier.Long.newBuilder().setValue(total).build());
        responseObserver.onCompleted();
    }

    protected abstract long deleteLocalCrawl(String crawlID);
}
