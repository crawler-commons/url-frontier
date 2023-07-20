// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "ListCrawls", description = "Prints out list of crawls")
public class ListCrawls implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-l", "--local"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description =
                    "restricts the scope to this frontier instance instead of aggregating over the cluster")
    private Boolean local;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
        StringList crawlIDs =
                blockingFrontier.listCrawls(
                        crawlercommons.urlfrontier.Urlfrontier.Local.newBuilder()
                                .setLocal(local)
                                .build());

        crawlIDs.getValuesList()
                .forEach(
                        s -> {
                            System.out.println(s);
                        });

        channel.shutdownNow();
    }
}
