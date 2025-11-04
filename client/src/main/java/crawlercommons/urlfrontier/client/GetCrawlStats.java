// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.GetCrawlStatsResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "GetCrawlStats",
        description = "Prints out the total number of URLs for a given crawl")
public class GetCrawlStats implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-c", "--crawlID"},
            required = true,
            defaultValue = "DEFAULT",
            paramLabel = "STRING",
            description = "crawl to get the total URL count for")
    private String crawl;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);
        Urlfrontier.GetCrawlStatsParams.Builder builder =
                Urlfrontier.GetCrawlStatsParams.newBuilder();

        builder.setCrawlID(crawl);
        GetCrawlStatsResponse s = blockingFrontier.getCrawlStats(builder.build());

        System.out.println(
                "Crawl statistics: "
                        + s.getCompletedURL()
                        + " completed, "
                        + s.getTotalURL()
                        + " total URLs");

        channel.shutdownNow();
    }
}
