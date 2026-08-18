// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import com.google.protobuf.util.JsonFormat;
import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.client.Client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Checks that a dump made with the client's DumpURLs command can be re-imported with PutURLs
 * without losing anything: URLs, queue keys, crawl IDs, metadata and scheduling information.
 */
public class DumpURLsTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DumpURLsTest.class);

    private String host;
    private String port;

    private ManagedChannel channel;

    private URLFrontierStub frontier;

    private URLFrontierBlockingStub blockingFrontier;

    @Before
    public void init() throws IOException {

        host = System.getProperty("urlfrontier.host", "localhost");
        port = System.getProperty("urlfrontier.port", "7071");

        LOG.info("Initialisation of connection to URLFrontier service on {}:{}", host, port);

        channel =
                ManagedChannelBuilder.forAddress(host, Integer.parseInt(port))
                        .usePlaintext()
                        .build();
        frontier = URLFrontierGrpc.newStub(channel);
        blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        deleteAllCrawls();
    }

    @After
    public void shutdown() {
        deleteAllCrawls();
        channel.shutdown();
    }

    private void deleteAllCrawls() {
        StringList crawlids =
                blockingFrontier.listCrawls(Urlfrontier.Local.newBuilder().setLocal(true).build());
        for (String crawlid : crawlids.getValuesList()) {
            blockingFrontier.deleteCrawl(
                    Urlfrontier.DeleteCrawlMessage.newBuilder().setValue(crawlid).build());
        }
    }

    @Test
    public void roundTrip() throws IOException {

        final long refetchDate = Instant.now().getEpochSecond() + 3600;

        // a URL with custom metadata, one completed, one scheduled for refetch and one
        // in a separate crawl
        URLInfo withMetadata =
                URLInfo.newBuilder()
                        .setUrl("http://example.com/discovered")
                        .setKey("example.com")
                        .putMetadata("source", StringList.newBuilder().addValues("seed").build())
                        .build();
        URLInfo done =
                URLInfo.newBuilder()
                        .setUrl("http://example.com/done")
                        .setKey("example.com")
                        .build();
        URLInfo later =
                URLInfo.newBuilder()
                        .setUrl("http://example.com/later")
                        .setKey("example.com")
                        .build();
        URLInfo bespoke =
                URLInfo.newBuilder()
                        .setUrl("http://bespoke.com/")
                        .setKey("bespoke.com")
                        .setCrawlID("BESPOKE")
                        .build();

        int acked =
                sendURLs(
                        URLItem.newBuilder()
                                .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(withMetadata))
                                .build(),
                        URLItem.newBuilder()
                                .setKnown(KnownURLItem.newBuilder().setInfo(done))
                                .build(),
                        URLItem.newBuilder()
                                .setKnown(
                                        KnownURLItem.newBuilder()
                                                .setInfo(later)
                                                .setRefetchableFromDate(refetchDate))
                                .build(),
                        URLItem.newBuilder()
                                .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(bespoke))
                                .build());

        Assert.assertEquals("incorrect number of URLs acked", 4, acked);

        // dump the whole frontier with the client command
        Path dump = Files.createTempFile("urlfrontier-dump", ".jsonl");
        try {
            int exitCode =
                    new CommandLine(new Client())
                            .execute("-t", host, "-p", port, "DumpURLs", "-o", dump.toString());
            Assert.assertEquals("DumpURLs failed", 0, exitCode);

            List<String> lines = Files.readAllLines(dump, StandardCharsets.UTF_8);
            Assert.assertEquals("incorrect number of URLs dumped", 4, lines.size());

            // every line must parse back as a URLItem, the format PutURLs takes
            for (String line : lines) {
                URLItem.Builder builder = URLItem.newBuilder();
                JsonFormat.parser().merge(line, builder);
                Assert.assertTrue("dumped item is not a known URL", builder.hasKnown());
            }

            // empty the frontier before re-importing
            deleteAllCrawls();
            StringList crawlids =
                    blockingFrontier.listCrawls(
                            Urlfrontier.Local.newBuilder().setLocal(true).build());
            Assert.assertEquals("frontier not empty", 0, crawlids.getValuesCount());

            exitCode =
                    new CommandLine(new Client())
                            .execute("-t", host, "-p", port, "PutURLs", "-f", dump.toString());
            Assert.assertEquals("PutURLs failed", 0, exitCode);
        } finally {
            Files.deleteIfExists(dump);
        }

        // the completed URL must still be completed
        URLItem status =
                blockingFrontier.getURLStatus(
                        URLStatusRequest.newBuilder()
                                .setUrl("http://example.com/done")
                                .setKey("example.com")
                                .setCrawlID(CrawlID.DEFAULT)
                                .build());
        Assert.assertEquals(
                "refetch date of completed URL not preserved",
                0,
                status.getKnown().getRefetchableFromDate());

        // the scheduled URL must have kept its refetch date
        status =
                blockingFrontier.getURLStatus(
                        URLStatusRequest.newBuilder()
                                .setUrl("http://example.com/later")
                                .setKey("example.com")
                                .setCrawlID(CrawlID.DEFAULT)
                                .build());
        Assert.assertEquals(
                "refetch date of scheduled URL not preserved",
                refetchDate,
                status.getKnown().getRefetchableFromDate());

        // the metadata must have survived the round trip
        status =
                blockingFrontier.getURLStatus(
                        URLStatusRequest.newBuilder()
                                .setUrl("http://example.com/discovered")
                                .setKey("example.com")
                                .setCrawlID(CrawlID.DEFAULT)
                                .build());
        Assert.assertEquals(
                "metadata not preserved",
                "seed",
                status.getKnown().getInfo().getMetadataOrThrow("source").getValues(0));

        // the separate crawl must be back as well
        Urlfrontier.Long count =
                blockingFrontier.countURLs(
                        Urlfrontier.CountUrlParams.newBuilder().setCrawlID("BESPOKE").build());
        Assert.assertEquals("incorrect number of URLs in BESPOKE crawl", 1, count.getValue());
    }

    @Test
    public void parallelDump() throws IOException {

        // spread the URLs over several queues in two crawls
        URLItem[] items = new URLItem[21];
        for (int i = 0; i < 20; i++) {
            URLInfo info =
                    URLInfo.newBuilder().setUrl("http://site" + (i % 10) + ".com/page" + i).build();
            items[i] =
                    URLItem.newBuilder()
                            .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info))
                            .build();
        }
        items[20] =
                URLItem.newBuilder()
                        .setDiscovered(
                                DiscoveredURLItem.newBuilder()
                                        .setInfo(
                                                URLInfo.newBuilder()
                                                        .setUrl("http://bespoke.com/")
                                                        .setCrawlID("BESPOKE")))
                        .build();

        int acked = sendURLs(items);
        Assert.assertEquals("incorrect number of URLs acked", 21, acked);

        Path dump = Files.createTempFile("urlfrontier-dump", ".jsonl");
        try {
            int exitCode =
                    new CommandLine(new Client())
                            .execute(
                                    "-t",
                                    host,
                                    "-p",
                                    port,
                                    "DumpURLs",
                                    "-o",
                                    dump.toString(),
                                    "-t",
                                    "4");
            Assert.assertEquals("DumpURLs failed", 0, exitCode);

            List<String> lines = Files.readAllLines(dump, StandardCharsets.UTF_8);
            Assert.assertEquals("incorrect number of URLs dumped", 21, lines.size());

            deleteAllCrawls();

            exitCode =
                    new CommandLine(new Client())
                            .execute("-t", host, "-p", port, "PutURLs", "-f", dump.toString());
            Assert.assertEquals("PutURLs failed", 0, exitCode);
        } finally {
            Files.deleteIfExists(dump);
        }

        Urlfrontier.Long count =
                blockingFrontier.countURLs(
                        Urlfrontier.CountUrlParams.newBuilder()
                                .setCrawlID(CrawlID.DEFAULT)
                                .build());
        Assert.assertEquals("incorrect number of URLs in default crawl", 20, count.getValue());

        count =
                blockingFrontier.countURLs(
                        Urlfrontier.CountUrlParams.newBuilder().setCrawlID("BESPOKE").build());
        Assert.assertEquals("incorrect number of URLs in BESPOKE crawl", 1, count.getValue());
    }

    private final int sendURLs(URLItem... items) {
        final AtomicBoolean completed = new AtomicBoolean(false);
        final AtomicInteger acked = new AtomicInteger(0);

        StreamObserver<Urlfrontier.AckMessage> responseObserver =
                new StreamObserver<>() {

                    @Override
                    public void onNext(Urlfrontier.AckMessage value) {
                        acked.addAndGet(1);
                    }

                    @Override
                    public void onError(Throwable t) {
                        completed.set(true);
                        LOG.info("Error received", t);
                    }

                    @Override
                    public void onCompleted() {
                        completed.set(true);
                    }
                };

        StreamObserver<URLItem> streamObserver = frontier.putURLs(responseObserver);

        for (URLItem item : items) {
            streamObserver.onNext(item);
        }

        streamObserver.onCompleted();

        // wait for completion
        while (completed.get() == false) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return acked.get();
    }
}
