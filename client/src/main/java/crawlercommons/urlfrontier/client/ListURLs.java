package crawlercommons.urlfrontier.client;

import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.Pagination.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "ListURLs", description = "Prints out URLs")
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

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();
        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        if (output.length() > 0) {

            File f = new File(output);
            f.delete();
            CharSink sink = Files.asCharSink(f, Charset.defaultCharset(), FileWriteMode.APPEND);

            while (true) {

                Builder builder = crawlercommons.urlfrontier.Urlfrontier.Pagination.newBuilder();
                builder.setLocal(local);
                builder.setSize(maxNumURLs);
                builder.setStart(start);
                builder.setIncludeInactive(true);
                builder.setCrawlID(crawl);

                Iterator<URLItem> it = blockingFrontier.listURLs(builder.build());
                while (it.hasNext()) {
                    try {
                        URLItem item = it.next();
                        sink.write(
                                item.getKnown().getInfo().getUrl()
                                        + ";"
                                        + item.getKnown().getRefetchableFromDate());
                        sink.write("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        } else {
            Builder builder = crawlercommons.urlfrontier.Urlfrontier.Pagination.newBuilder();
            builder.setLocal(local);
            builder.setSize(maxNumURLs);
            builder.setStart(start);
            builder.setIncludeInactive(true);
            builder.setCrawlID(crawl);

            Iterator<URLItem> it = blockingFrontier.listURLs(builder.build());
            while (it.hasNext()) {

                URLItem item = it.next();
                System.out.println(
                        item.getKnown().getInfo().getUrl()
                                + ";"
                                + item.getKnown().getRefetchableFromDate());
            }
        }
        channel.shutdownNow();
    }
}
