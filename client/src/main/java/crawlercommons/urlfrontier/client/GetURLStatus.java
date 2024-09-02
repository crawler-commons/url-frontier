package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "GetURLStatus", description = "Get the status of an URL")
public class GetURLStatus implements Runnable {

    @ParentCommand private Client parent;

    @ArgGroup(exclusive = false, multiplicity = "1")
    Dependent group;

    static class Dependent {
        @Option(
                names = {"-c", "--crawlID"},
                required = true,
                paramLabel = "STRING",
                description = "crawl of the url to be checked")
        private String crawl;

        @Option(
                names = {"-k", "--key"},
                required = true,
                paramLabel = "STRING",
                description = "key to use to target a specific queue")
        private String key;

        @Option(
                names = {"-u", "--url"},
                required = true,
                paramLabel = "STRING",
                description = "url to check for")
        private String url;
    }

    @Option(
            names = {"-p", "--parsedate"},
            description = "Print the Epoch refetch time in local time")
    private boolean parse = false;

    // Use the system default time zone
    private ZoneId zoneId = ZoneId.systemDefault();

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();
        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        System.out.println(
                "Checking status of URL " + group.url + " (crawlId = " + group.crawl + ")");
        URLStatusRequest request =
                URLStatusRequest.newBuilder()
                        .setUrl(group.url)
                        .setKey(group.key)
                        .setCrawlID(group.crawl)
                        .build();

        try {
            URLItem item = blockingFrontier.getURLStatus(request);

            if (parse) {
                Instant instant = Instant.ofEpochSecond(item.getKnown().getRefetchableFromDate());
                LocalDateTime localDate = instant.atZone(zoneId).toLocalDateTime();
                System.out.println(item.getKnown().getInfo().getUrl() + ";" + localDate);
            } else {
                System.out.println(
                        item.getKnown().getInfo().getUrl()
                                + ";"
                                + item.getKnown().getRefetchableFromDate());
            }
        } catch (StatusRuntimeException sre) {
            if (sre.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                System.out.println("URL is not in frontier: " + group.url);
            } else {
                // Handle other errors
                System.err.println(sre.getMessage());
            }
        } catch (Exception t) {
            // Handle other errors
            System.err.println(t.getMessage());
        }

        channel.shutdownNow();
    }
}
