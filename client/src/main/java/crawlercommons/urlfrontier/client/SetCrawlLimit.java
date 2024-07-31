package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine;

@CommandLine.Command(name = "SetCrawlLimit", description = "Set crawl limit for specific queue")
public class SetCrawlLimit implements Runnable {
    @CommandLine.ParentCommand private Client parent;

    @CommandLine.Option(
            names = {"-k", "--key"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "key for queue to set limit for")
    private String key;

    @CommandLine.Option(
            names = {"-c", "--crawlID"},
            defaultValue = "DEFAULT",
            paramLabel = "STRING",
            description = "crawl to set limits for")
    private String crawl;

    @CommandLine.Option(
            names = {"-l", "--limit"},
            defaultValue = "0",
            paramLabel = "INT",
            description = "the limit. (0 to disable)")
    private int limit;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierGrpc.URLFrontierBlockingStub blockingFrontier =
                URLFrontierGrpc.newBlockingStub(channel);

        Urlfrontier.CrawlLimitParams.Builder builder = Urlfrontier.CrawlLimitParams.newBuilder();
        builder.setCrawlID(crawl);
        builder.setKey(key);
        builder.setLimit(limit);

        blockingFrontier.setCrawlLimit(builder.build());

        channel.shutdownNow();
    }
}
