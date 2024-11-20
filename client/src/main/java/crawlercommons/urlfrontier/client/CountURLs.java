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

@Command(name = "CountURLs", description = "Counts the number of URLs in a Frontier")
public class CountURLs implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-k", "--key"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "key of the queue to get the URL count for")
    private String key;

    @Option(
            names = {"-c", "--crawlID"},
            defaultValue = "DEFAULT",
            paramLabel = "STRING",
            description = "crawl to get the URL count for")
    private String crawl;

    @Option(
            names = {"-l", "--local"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description =
                    "restricts the scope to this frontier instance instead of aggregating over the cluster")
    private Boolean local;

    @Option(
            names = {"-f", "--filter"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "String filter applied to URLs")
    private String filter;

    @Option(
            names = {"-i", "--ignore-case"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description = "Ignore case sensitivity for search filter")
    private Boolean ignoreCase;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        Urlfrontier.CountUrlParams.Builder builder = Urlfrontier.CountUrlParams.newBuilder();

        if (key.length() > 0) {
            builder.setKey(key);
        }

        builder.setCrawlID(crawl);
        builder.setLocal(local);

        builder.setFilter(filter);
        builder.setIgnoreCase(ignoreCase);

        builder.setFilter(filter);

        Long s = blockingFrontier.countURLs(builder.build());
        System.out.println(s.getValue() + " URLs in frontier");

        channel.shutdownNow();
    }
}
