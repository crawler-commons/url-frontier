// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Iterator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "ListURLs",
        description = "Prints out all URLs in the Frontier",
        sortOptions = false)
public class ListURLs implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-n", "--number_urls"},
            defaultValue = "100",
            paramLabel = "NUM",
            description = "maximum number of URLs to return (default 100)")
    private int maxNumURLs;

    @Option(
            names = {"-s", "--start"},
            defaultValue = "0",
            paramLabel = "NUM",
            description = "starting position of URL to return (default 0)")
    private int start;

    @Option(
            names = {"-k", "--key"},
            required = false,
            paramLabel = "STRING",
            description = "key to use to target a specific queue")
    private String key;

    @Option(
            names = {"-o", "--output"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "output file to dump all the URLs")
    private String output;

    @Option(
            names = {"-c", "--crawlID"},
            defaultValue = "DEFAULT",
            paramLabel = "STRING",
            description = "crawl to get the queues for")
    private String crawl;

    @Option(
            names = {"-l", "--local"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description =
                    "restricts the scope to this frontier instance instead of aggregating over the cluster")
    private Boolean local;

    @Option(
            names = {"-j", "--json"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description = "Outputs in JSON format")
    private Boolean json;

    @Option(
            names = {"-p", "--parsedate"},
            defaultValue = "false",
            description = {
                "Print the refetch date in local time zone",
                "By default, time is UTC seconds since the Unix epoch",
                "Ignored if JSON output is selected"
            })
    private boolean parse;

    // Use the system default time zone
    private ZoneId zoneId = ZoneId.systemDefault();

    @Override
    public void run() {

        Builder builder = crawlercommons.urlfrontier.Urlfrontier.ListUrlParams.newBuilder();
        builder.setLocal(local);
        if (key != null) {
            builder.setKey(key);
        }
        builder.setSize(maxNumURLs);
        builder.setStart(start);
        builder.setCrawlID(crawl);

        PrintStream outstream = null;
        if (output.length() > 0) {
            File f = new File(output);
            try {
                Files.deleteIfExists(f.toPath());
                outstream = new PrintStream(f, Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace(System.err);
                return;
            }
        } else {
            outstream = System.out;
        }

        Printer jprinter = JsonFormat.printer();

        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();
        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        Iterator<URLItem> it = blockingFrontier.listURLs(builder.build());
        while (it.hasNext()) {

            URLItem item = it.next();

            String fetchDate;
            if (parse) {
                Instant instant = Instant.ofEpochSecond(item.getKnown().getRefetchableFromDate());
                LocalDateTime localDate = instant.atZone(zoneId).toLocalDateTime();
                fetchDate = localDate.toString();
            } else {
                fetchDate = String.valueOf(item.getKnown().getRefetchableFromDate());
            }

            if (Boolean.TRUE.equals(json)) {
                try {
                    outstream.println(jprinter.print(item));
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace(System.err);
                    break;
                }
            } else {
                outstream.println(item.getKnown().getInfo().getUrl() + ";" + fetchDate);
            }
        }

        outstream.close();
        channel.shutdownNow();
    }
}
