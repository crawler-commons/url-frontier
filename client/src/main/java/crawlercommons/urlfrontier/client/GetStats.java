// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Map;
import java.util.Map.Entry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "GetStats", description = "Prints out stats from the Frontier")
public class GetStats implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-k", "--key"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "restrict the stats to a specific queue")
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

        Stats s = blockingFrontier.getStats(builder.build());
        System.out.println("Number of queues: " + s.getNumberOfQueues());
        System.out.println("Active URLs: " + s.getSize());
        System.out.println("In process: " + s.getInProcess());

        Map<String, Long> counts = s.getCountsMap();
        for (Entry<String, Long> kv : counts.entrySet()) {
            System.out.println(kv.getKey() + " = " + kv.getValue());
        }

        channel.shutdownNow();
    }
}
