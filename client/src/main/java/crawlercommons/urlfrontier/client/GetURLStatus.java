// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest.Builder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "GetURLStatus", description = "Get the status of an URL", sortOptions = false)
public class GetURLStatus implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-c", "--crawlID"},
            required = false,
            defaultValue = CrawlID.DEFAULT,
            paramLabel = "STRING",
            description = "crawl of the url to be checked")
    private String crawl;

    @Option(
            names = {"-k", "--key"},
            required = false,
            paramLabel = "STRING",
            description = "key to use to target a specific queue")
    private String key;

    @Option(
            names = {"-u", "--url"},
            required = true,
            paramLabel = "STRING",
            description = "url to check for")
    private String url;

    @Option(
            names = {"-p", "--parsedate"},
            description = {
                "Print the refetch date in local time zone",
                "By default, time is in UTC seconds since the Unix epoch"
            })
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

        Builder builder = URLStatusRequest.newBuilder().setUrl(url).setCrawlID(crawl);

        String s1 = String.format("Checking status of URL %s (crawlId = %s)", url, crawl);
        if (key != null) {
            s1 += String.format(" (key = %s)", key);
            builder.setKey(key);
        }
        System.out.println(s1);

        URLStatusRequest request = builder.build();

        try {
            URLItem item = blockingFrontier.getURLStatus(request);
            String fetchDate;

            if (parse) {
                Instant instant = Instant.ofEpochSecond(item.getKnown().getRefetchableFromDate());
                LocalDateTime localDate = instant.atZone(zoneId).toLocalDateTime();
                fetchDate = localDate.toString();
            } else {
                fetchDate = String.valueOf(item.getKnown().getRefetchableFromDate());
            }
            System.out.println(item.getKnown().getInfo().getUrl() + ";" + fetchDate);

        } catch (StatusRuntimeException sre) {
            if (sre.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                System.out.println("URL is not in frontier: " + url);
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
