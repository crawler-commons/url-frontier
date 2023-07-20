// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.Long;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "DeleteQueue", description = "Delete a queue from the Frontier")
public class DeleteQueue implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-k", "--key"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "key for the queue to be deleted")
    private String key;

    @Option(
            names = {"-c", "--crawlID"},
            defaultValue = "DEFAULT",
            paramLabel = "STRING",
            description = "crawl to get the stats for")
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

        Urlfrontier.QueueWithinCrawlParams.Builder builder =
                Urlfrontier.QueueWithinCrawlParams.newBuilder();

        if (key.length() > 0) {
            builder.setKey(key);
        }

        builder.setCrawlID(crawl);
        builder.setLocal(local);

        Long s = blockingFrontier.deleteQueue(builder.build());
        System.out.println(s.getValue() + " URLs deleted");

        channel.shutdownNow();
    }
}
