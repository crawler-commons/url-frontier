// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.Long;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "DeleteCrawl", description = "Delete an entire crawl from the Frontier")
public class DeleteCrawl implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-c", "--crawlID"},
            required = true,
            paramLabel = "STRING",
            description = "crawl to delete")
    private String crawl;

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

        Long s =
                blockingFrontier.deleteCrawl(
                        crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage.newBuilder()
                                .setValue(crawl)
                                .setLocal(local)
                                .build());
        System.out.println(s.getValue() + " URLs deleted");

        channel.shutdownNow();
    }
}
