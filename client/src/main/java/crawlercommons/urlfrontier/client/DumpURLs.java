// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.JsonFormat.Printer;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.ListUrlParams;
import crawlercommons.urlfrontier.Urlfrontier.Local;
import crawlercommons.urlfrontier.Urlfrontier.Pagination;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "DumpURLs",
        description = {
            "Export all the URLs of a Frontier as JSON, one per line, in the format taken by"
                    + " PutURLs; used to migrate the data to a different backend or version.",
            "The dump covers only the node the client connects to: in a cluster, dump every node"
                    + " and concatenate the files.",
            "Deactivate the Frontier and stop the URL injection first if the dump must be"
                    + " complete."
        })
public class DumpURLs implements Callable<Integer> {

    /** how many URLs between progress messages */
    private static final long REPORT_EVERY = 100_000;

    /** how many queue names are retrieved per ListQueues call */
    private static final int QUEUE_PAGE = 10_000;

    @ParentCommand private Client parent;

    @Option(
            names = {"-o", "--output"},
            defaultValue = "",
            paramLabel = "STRING",
            description =
                    "output file for the dump, compressed with gzip if its name ends in .gz;"
                            + " defaults to the standard output")
    private String output;

    @Option(
            names = {"-c", "--crawlID"},
            required = false,
            paramLabel = "STRING",
            description = "restrict the dump to this crawl; by default every crawl is dumped")
    private String crawl;

    @Option(
            names = {"-t", "--threads"},
            defaultValue = "1",
            paramLabel = "NUM",
            description =
                    "number of threads dumping the content queue by queue, each on its own"
                            + " connection (default to 1, i.e. each crawl is streamed whole in a"
                            + " single call)")
    private int threads;

    /** PutURLs reads the dump back one line at a time */
    private final Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();

    private final AtomicLong total = new AtomicLong();

    private Writer out;

    @Override
    public Integer call() {

        final ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        try {
            final URLFrontierBlockingStub blockingFrontier =
                    URLFrontierGrpc.newBlockingStub(channel);

            // the URLs come from the node we are connected to, so take the crawl IDs
            // from that same node
            final List<String> crawlIDs;
            if (crawl != null) {
                crawlIDs = List.of(crawl);
            } else {
                crawlIDs =
                        blockingFrontier
                                .listCrawls(Local.newBuilder().setLocal(true).build())
                                .getValuesList();
            }

            final Instant start = Instant.now();
            final boolean toFile = !output.isEmpty();

            try {
                OutputStream os = toFile ? new FileOutputStream(output) : System.out;
                if (toFile && output.endsWith(".gz")) {
                    os = new GZIPOutputStream(os);
                }
                out = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

                final boolean success;
                if (threads > 1) {
                    success = dumpParallel(blockingFrontier, crawlIDs);
                } else {
                    success = dumpSequential(blockingFrontier, crawlIDs);
                }

                if (toFile) {
                    out.close();
                } else {
                    out.flush();
                }

                if (!success) {
                    System.err.println("The dump is incomplete");
                    return 1;
                }
            } catch (IOException e) {
                System.err.println(
                        "Error while writing "
                                + (toFile ? output : "to the standard output")
                                + ": "
                                + e.getMessage());
                return 1;
            }

            long timetaken = Instant.now().toEpochMilli() - start.toEpochMilli();
            System.err.println("Total: " + total.get() + " URLs dumped in " + timetaken + " msec");

            return 0;
        } catch (StatusRuntimeException e) {
            System.err.println("Error while dumping the URLs: " + e.getMessage());
            return 1;
        } finally {
            channel.shutdownNow();
        }
    }

    /** Streams each crawl whole, in a single call */
    private boolean dumpSequential(
            final URLFrontierBlockingStub blockingFrontier, final List<String> crawlIDs)
            throws IOException {
        for (String crawlID : crawlIDs) {
            long count = 0;
            ListUrlParams params =
                    ListUrlParams.newBuilder()
                            .setCrawlID(crawlID)
                            .setSize(Integer.MAX_VALUE)
                            .build();
            Iterator<URLItem> it = blockingFrontier.listURLs(params);
            while (it.hasNext()) {
                writeLine(printer.print(it.next()));
                count++;
            }
            System.err.println(count + " URLs dumped for crawl " + crawlID);
        }
        return true;
    }

    /**
     * Lists the queues of every crawl, then dumps them queue by queue with the threads sharing the
     * list; returns false if any of them failed.
     */
    private boolean dumpParallel(
            final URLFrontierBlockingStub blockingFrontier, final List<String> crawlIDs) {

        // [crawlID, queue] pairs, taken by whichever thread is free
        final ConcurrentLinkedQueue<String[]> work = new ConcurrentLinkedQueue<>();

        for (String crawlID : crawlIDs) {
            int startpos = 0;
            int queues = 0;
            while (true) {
                QueueList page =
                        blockingFrontier.listQueues(
                                Pagination.newBuilder()
                                        .setCrawlID(crawlID)
                                        .setIncludeInactive(true)
                                        .setLocal(true)
                                        .setStart(startpos)
                                        .setSize(QUEUE_PAGE)
                                        .build());
                for (String queue : page.getValuesList()) {
                    work.add(new String[] {crawlID, queue});
                }
                queues += page.getValuesCount();
                startpos += page.getValuesCount();
                if (page.getValuesCount() < QUEUE_PAGE) {
                    break;
                }
            }
            System.err.println(queues + " queues to dump for crawl " + crawlID);
        }

        final AtomicBoolean failed = new AtomicBoolean(false);

        final List<Thread> workers = new ArrayList<>(threads);
        for (int i = 0; i < threads; i++) {
            Thread worker =
                    new Thread(
                            () -> {
                                final ManagedChannel channel =
                                        ManagedChannelBuilder.forAddress(
                                                        parent.hostname, parent.port)
                                                .usePlaintext()
                                                .build();
                                try {
                                    final URLFrontierBlockingStub stub =
                                            URLFrontierGrpc.newBlockingStub(channel);
                                    String[] item;
                                    while (!failed.get() && (item = work.poll()) != null) {
                                        ListUrlParams params =
                                                ListUrlParams.newBuilder()
                                                        .setCrawlID(item[0])
                                                        .setKey(item[1])
                                                        .setSize(Integer.MAX_VALUE)
                                                        .build();
                                        Iterator<URLItem> it = stub.listURLs(params);
                                        while (it.hasNext()) {
                                            writeLine(printer.print(it.next()));
                                        }
                                    }
                                } catch (StatusRuntimeException | IOException e) {
                                    failed.set(true);
                                    System.err.println(
                                            "Error while dumping the URLs: " + e.getMessage());
                                } finally {
                                    channel.shutdownNow();
                                }
                            },
                            "DumpURLs-" + i);
            workers.add(worker);
            worker.start();
        }

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return !failed.get();
    }

    private void writeLine(final String line) throws IOException {
        synchronized (out) {
            out.write(line);
            out.write('\n');
        }
        long soFar = total.incrementAndGet();
        if (soFar % REPORT_EVERY == 0) {
            System.err.println(soFar + " URLs dumped so far...");
        }
    }
}
