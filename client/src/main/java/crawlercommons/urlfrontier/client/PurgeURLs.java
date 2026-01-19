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

@Command(name = "PurgeURLs", description = "Purge old URLs in a Frontier")
public class PurgeURLs implements Runnable {

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
            names = {"-d", "--days"},
            defaultValue = "",
            paramLabel = "NUM",
            description = "Number of days after which we want to delete URLs",
            required = true)
    private int days;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        Urlfrontier.PurgeUrlParams.Builder builder = Urlfrontier.PurgeUrlParams.newBuilder();

        if (key.length() > 0) {
            builder.setKey(key);
        }

        builder.setCrawlID(crawl);
        builder.setDays(days);

        Long s = blockingFrontier.purgeURLs(builder.build());
        System.out.println("Purged " + s.getValue() + " URLs in frontier");

        channel.shutdownNow();
    }
}
