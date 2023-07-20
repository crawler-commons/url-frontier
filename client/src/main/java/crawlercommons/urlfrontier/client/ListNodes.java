// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(name = "ListNodes", description = "Prints out list of nodes forming the cluster")
public class ListNodes implements Runnable {

    @ParentCommand private Client parent;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        crawlercommons.urlfrontier.Urlfrontier.Empty.Builder builder =
                crawlercommons.urlfrontier.Urlfrontier.Empty.newBuilder();

        StringList crawlIDs = blockingFrontier.listNodes(builder.build());

        crawlIDs.getValuesList()
                .forEach(
                        s -> {
                            System.out.println(s);
                        });

        channel.shutdownNow();
    }
}
