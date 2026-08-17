// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import crawlercommons.urlfrontier.CrawlID;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.BatchAck;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredBatch;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "PutURLs", description = "Send URLs from a file into a Frontier")
public class PutURLs implements Callable<Integer> {

    /** how often the intermediate throughput is displayed */
    private static final long REPORT_INTERVAL_SEC = 30;

    @ParentCommand private Client parent;

    @Option(
            names = {"-f", "--file"},
            required = true,
            paramLabel = "STRING",
            description = "path to file containing the URLs to inject into the Frontier")
    private String file;

    @Option(
            names = {"-c", "--crawlID"},
            defaultValue = CrawlID.DEFAULT,
            paramLabel = "STRING",
            description = "crawl to get the stats for")
    private String crawl;

    @Option(
            names = {"-t", "--threads"},
            defaultValue = "1",
            paramLabel = "NUM",
            description =
                    "number of threads sending URLs, each on its own stream (default to 1). The"
                            + " URLs are shared between them, they are not sent more than once.")
    private int threads;

    @Option(
            names = {"-w", "--in-flight"},
            defaultValue = "10000",
            paramLabel = "NUM",
            description =
                    "maximum number of URLs sent on a stream but not confirmed yet, per thread"
                            + " (default to 10000)")
    private int inFlight;

    @Option(
            names = {"-b", "--batch"},
            defaultValue = "0",
            paramLabel = "NUM",
            description =
                    "number of discovered URLs to send per message through the PutDiscovered"
                            + " endpoint, which amortises the per-message cost (default to 0,"
                            + " i.e. every URL is sent individually through PutURLs). Known URLs"
                            + " always go through PutURLs. Falls back to sending everything"
                            + " individually if the server has no PutDiscovered.")
    private int batch;

    /** value of {@link #batch} actually applied, 0 when the server has no PutDiscovered */
    private int effectiveBatch;

    @Override
    public Integer call() {

        final int streams = Math.max(1, threads);

        effectiveBatch = batch;
        if (effectiveBatch > 0 && !serverSupportsPutDiscovered()) {
            System.err.println(
                    "Server does not support PutDiscovered - sending the URLs individually");
            effectiveBatch = 0;
        }

        final AtomicInteger sent = new AtomicInteger(0);
        final AtomicInteger acked = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);
        final AtomicInteger skipped = new AtomicInteger(0);
        final AtomicInteger ok = new AtomicInteger(0);

        // set when any of the streams is terminated by an error
        final AtomicBoolean streamError = new AtomicBoolean(false);
        final AtomicBoolean readError = new AtomicBoolean(false);

        Instant start = Instant.now();

        // reports the throughput at regular intervals so that any degradation over time is visible
        ScheduledExecutorService reporter =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "PutURLs-OPS-reporter");
                            t.setDaemon(true);
                            return t;
                        });

        final AtomicInteger lastAcked = new AtomicInteger(0);
        final AtomicLong lastReport = new AtomicLong(start.toEpochMilli());

        reporter.scheduleAtFixedRate(
                () -> {
                    long now = Instant.now().toEpochMilli();
                    long elapsed = now - lastReport.getAndSet(now);
                    int current = acked.get();
                    int delta = current - lastAcked.getAndSet(current);
                    System.out.println(
                            String.format(
                                    "Acked: %d - OPS over the last %d sec: %.2f",
                                    current,
                                    Math.round(elapsed / 1000.0),
                                    delta * 1000.0 / Math.max(1, elapsed)));
                },
                REPORT_INTERVAL_SEC,
                REPORT_INTERVAL_SEC,
                TimeUnit.SECONDS);

        // the file is read once and the lines handed out in chunks, so that adding threads
        // multiplies the parsing and sending but not the reading
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(file))) {

            final LineSource lines = new LineSource(reader);

            final List<Thread> senders = new ArrayList<>(streams);
            for (int i = 0; i < streams; i++) {
                Thread sender =
                        new Thread(
                                () ->
                                        sendFrom(
                                                lines,
                                                sent,
                                                acked,
                                                ok,
                                                skipped,
                                                failed,
                                                streamError,
                                                readError),
                                "PutURLs-sender-" + i);
                senders.add(sender);
                sender.start();
            }

            for (Thread sender : senders) {
                sender.join();
            }

        } catch (IOException e1) {
            readError.set(true);
            System.err.println("Error while reading " + file + ": " + e1.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        reporter.shutdownNow();

        long timetaken = Instant.now().toEpochMilli() - start.toEpochMilli();

        System.out.println("Sent: " + sent.get());
        System.out.println("Acked: " + acked.get());
        System.out.println("OK: " + ok.get());
        System.out.println("Skipped: " + skipped.get());
        System.out.println("Failed: " + failed.get());
        System.out.println("Total time: " + timetaken + " msec");
        System.out.println(
                String.format("Average OPS: %.2f", acked.get() * 1000.0 / Math.max(1, timetaken)));

        if (streamError.get()) {
            System.err.println(
                    "The connection to the Frontier failed, "
                            + (sent.get() - acked.get())
                            + " URLs out of "
                            + sent.get()
                            + " sent were not confirmed");
        }

        return (streamError.get() || readError.get()) ? 1 : 0;
    }

    /**
     * Probes the server with an empty batch: an old server answers the call itself with
     * UNIMPLEMENTED, a current one acks it.
     */
    private boolean serverSupportsPutDiscovered() {
        final ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();
        try {
            final CountDownLatch done = new CountDownLatch(1);
            final AtomicBoolean supported = new AtomicBoolean(false);
            StreamObserver<DiscoveredBatch> stream =
                    URLFrontierGrpc.newStub(channel)
                            .putDiscovered(
                                    new StreamObserver<BatchAck>() {
                                        @Override
                                        public void onNext(BatchAck value) {
                                            supported.set(true);
                                        }

                                        @Override
                                        public void onError(Throwable t) {
                                            done.countDown();
                                        }

                                        @Override
                                        public void onCompleted() {
                                            done.countDown();
                                        }
                                    });
            try {
                stream.onNext(DiscoveredBatch.newBuilder().setID("probe").build());
                stream.onCompleted();
            } catch (IllegalStateException e) {
                // the call got terminated straight away
            }
            try {
                done.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return supported.get();
        } finally {
            channel.shutdownNow();
        }
    }

    /**
     * Waits for room in the window and for the transport, then sends the buffered items as one
     * batch. Returns false when the stream died or the thread got interrupted.
     */
    private static boolean flushBatch(
            final List<URLInfo> buffer,
            final StreamObserver<DiscoveredBatch> batchStream,
            final ClientCallStreamObserver<DiscoveredBatch> transport,
            final Object flow,
            final int window,
            final AtomicInteger streamSent,
            final AtomicInteger streamAcked,
            final AtomicInteger sent,
            final AtomicBoolean thisStreamFailed,
            final AtomicInteger batchSequence) {
        if (buffer.isEmpty()) {
            return true;
        }
        try {
            synchronized (flow) {
                while (!thisStreamFailed.get()
                        && (streamSent.get() > streamAcked.get() + window
                                || (transport != null && !transport.isReady()))) {
                    flow.wait(100);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        if (thisStreamFailed.get()) {
            return false;
        }
        try {
            batchStream.onNext(
                    DiscoveredBatch.newBuilder()
                            .setID(Integer.toString(batchSequence.incrementAndGet()))
                            .addAllItems(buffer)
                            .build());
            streamSent.addAndGet(buffer.size());
            sent.addAndGet(buffer.size());
            buffer.clear();
            return true;
        } catch (IllegalStateException e) {
            // the stream got terminated while we were sending
            return false;
        }
    }

    /**
     * Sends URLs taken from the shared source on a stream of its own until the source is exhausted,
     * then waits for the Frontier to confirm every one of them.
     */
    private void sendFrom(
            final LineSource lines,
            final AtomicInteger sent,
            final AtomicInteger acked,
            final AtomicInteger ok,
            final AtomicInteger skipped,
            final AtomicInteger failed,
            final AtomicBoolean streamError,
            final AtomicBoolean readError) {

        final ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        try {
            final URLFrontierStub stub = URLFrontierGrpc.newStub(channel);

            // how far this stream is allowed to run ahead of the Frontier, counted on its
            // own so that a slow stream does not hold the others back
            final int window = Math.max(1, inFlight);
            final AtomicInteger streamSent = new AtomicInteger(0);
            final AtomicInteger streamAcked = new AtomicInteger(0);

            // counted down when the Frontier closes the streams, which it only does once
            // everything it received has been acked
            final CountDownLatch finished = new CountDownLatch(effectiveBatch > 0 ? 2 : 1);

            // errors on this stream only, the shared flag is for the exit code
            final AtomicBoolean thisStreamFailed = new AtomicBoolean(false);

            // the sender waits on this when it is not allowed to send: an ack frees a
            // slot in the window, the transport becoming ready frees the wire
            final Object flow = new Object();

            // the stream seen from the transport side, needed for isReady(); set before
            // putURLs returns
            final AtomicReference<ClientCallStreamObserver<URLItem>> requestStream =
                    new AtomicReference<>();

            ClientResponseObserver<URLItem, crawlercommons.urlfrontier.Urlfrontier.AckMessage>
                    responseObserver =
                            new ClientResponseObserver<>() {

                                @Override
                                public void beforeStart(ClientCallStreamObserver<URLItem> stream) {
                                    requestStream.set(stream);
                                    stream.setOnReadyHandler(
                                            () -> {
                                                synchronized (flow) {
                                                    flow.notifyAll();
                                                }
                                            });
                                }

                                @Override
                                public void onNext(
                                        crawlercommons.urlfrontier.Urlfrontier.AckMessage value) {
                                    // receives confirmation that the value has been received
                                    streamAcked.incrementAndGet();
                                    acked.addAndGet(1);
                                    if (value.getStatus().equals(AckMessage.Status.SKIPPED)) {
                                        skipped.getAndIncrement();
                                    } else if (value.getStatus().equals(AckMessage.Status.FAIL)) {
                                        failed.getAndIncrement();
                                    } else if (value.getStatus().equals(AckMessage.Status.OK)) {
                                        ok.getAndIncrement();
                                    }
                                    synchronized (flow) {
                                        flow.notifyAll();
                                    }
                                }

                                @Override
                                public void onError(Throwable t) {
                                    thisStreamFailed.set(true);
                                    streamError.set(true);
                                    System.err.println(
                                            "Error while sending the URLs: " + t.getMessage());
                                    finished.countDown();
                                    synchronized (flow) {
                                        flow.notifyAll();
                                    }
                                }

                                @Override
                                public void onCompleted() {
                                    finished.countDown();
                                    synchronized (flow) {
                                        flow.notifyAll();
                                    }
                                }
                            };

            final StreamObserver<URLItem> streamObserver = stub.putURLs(responseObserver);

            // the discovered URLs accumulate here and leave as one message per batch
            // on a PutDiscovered stream of their own; known URLs keep using PutURLs
            final List<URLInfo> buffer = new ArrayList<>(Math.max(1, effectiveBatch));
            final AtomicInteger batchSequence = new AtomicInteger();
            final AtomicReference<ClientCallStreamObserver<DiscoveredBatch>> batchTransport =
                    new AtomicReference<>();

            StreamObserver<DiscoveredBatch> batchStream = null;
            if (effectiveBatch > 0) {
                batchStream =
                        stub.putDiscovered(
                                new ClientResponseObserver<DiscoveredBatch, BatchAck>() {

                                    @Override
                                    public void beforeStart(
                                            ClientCallStreamObserver<DiscoveredBatch> stream) {
                                        batchTransport.set(stream);
                                        stream.setOnReadyHandler(
                                                () -> {
                                                    synchronized (flow) {
                                                        flow.notifyAll();
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onNext(BatchAck value) {
                                        final int n = value.getStatusesCount();
                                        streamAcked.addAndGet(n);
                                        acked.addAndGet(n);
                                        for (AckMessage.Status status : value.getStatusesList()) {
                                            if (status.equals(AckMessage.Status.SKIPPED)) {
                                                skipped.getAndIncrement();
                                            } else if (status.equals(AckMessage.Status.FAIL)) {
                                                failed.getAndIncrement();
                                            } else if (status.equals(AckMessage.Status.OK)) {
                                                ok.getAndIncrement();
                                            }
                                        }
                                        synchronized (flow) {
                                            flow.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        thisStreamFailed.set(true);
                                        streamError.set(true);
                                        System.err.println(
                                                "Error while sending the URLs: " + t.getMessage());
                                        finished.countDown();
                                        synchronized (flow) {
                                            flow.notifyAll();
                                        }
                                    }

                                    @Override
                                    public void onCompleted() {
                                        finished.countDown();
                                        synchronized (flow) {
                                            flow.notifyAll();
                                        }
                                    }
                                });
            }

            boolean interrupted = false;

            outer:
            while (!thisStreamFailed.get()) {

                final LineSource.Chunk chunk;
                try {
                    chunk = lines.next();
                } catch (IOException e) {
                    readError.set(true);
                    System.err.println("Error while reading " + file + ": " + e.getMessage());
                    break;
                }
                if (chunk == null) {
                    break;
                }

                for (int i = 0; i < chunk.lines.size(); i++) {

                    // the stream is dead, no point in sending anything else
                    if (thisStreamFailed.get()) {
                        break outer;
                    }

                    URLItem item = parse(chunk.lines.get(i), crawl);
                    if (item == null) {
                        System.err.println("Invalid input line " + (chunk.firstLineNumber + i));
                        continue;
                    }

                    // discovered URLs travel in batches when the option is on
                    if (effectiveBatch > 0 && item.hasDiscovered()) {
                        buffer.add(item.getDiscovered().getInfo());
                        if (buffer.size() >= effectiveBatch
                                && !flushBatch(
                                        buffer,
                                        batchStream,
                                        batchTransport.get(),
                                        flow,
                                        window,
                                        streamSent,
                                        streamAcked,
                                        sent,
                                        thisStreamFailed,
                                        batchSequence)) {
                            interrupted = Thread.currentThread().isInterrupted();
                            break outer;
                        }
                        continue;
                    }

                    // don't send too many in one go: wait for room in the window and
                    // for the transport to be able to take the item without buffering
                    // it, woken by the acks and by the transport itself. The timeout is
                    // a backstop, not a poll interval.
                    try {
                        final ClientCallStreamObserver<URLItem> transport = requestStream.get();
                        synchronized (flow) {
                            while (!thisStreamFailed.get()
                                    && (streamSent.get() > streamAcked.get() + window
                                            || (transport != null && !transport.isReady()))) {
                                flow.wait(100);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        interrupted = true;
                        break outer;
                    }

                    if (thisStreamFailed.get()) {
                        break outer;
                    }

                    try {
                        streamObserver.onNext(item);
                        streamSent.incrementAndGet();
                        sent.incrementAndGet();
                    } catch (IllegalStateException e) {
                        // the stream got terminated while we were sending
                        break outer;
                    }
                }
            }

            // whatever remains in the buffer goes out before the streams are closed
            if (effectiveBatch > 0 && !interrupted) {
                flushBatch(
                        buffer,
                        batchStream,
                        batchTransport.get(),
                        flow,
                        window,
                        streamSent,
                        streamAcked,
                        sent,
                        thisStreamFailed,
                        batchSequence);
            }

            // both streams are completed even if one of them failed: the healthy one
            // only counts the latch down once the server has closed it, which the
            // server only does after everything it received has been acked
            try {
                streamObserver.onCompleted();
            } catch (RuntimeException e) {
                // the stream got terminated in the meantime
            }
            if (batchStream != null) {
                try {
                    batchStream.onCompleted();
                } catch (RuntimeException e) {
                    // the stream got terminated in the meantime
                }
            }

            if (!interrupted) {
                try {
                    finished.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        } finally {
            channel.shutdownNow();
        }
    }

    /**
     * Hands out the lines of the input to the sender threads, a chunk at a time so that they are
     * not all queueing on the reader for every single line.
     */
    private static final class LineSource {

        /** how many lines a sender takes at once */
        private static final int CHUNK_SIZE = 512;

        private final BufferedReader reader;

        private int nextLineNumber = 0;

        private boolean exhausted = false;

        LineSource(BufferedReader reader) {
            this.reader = reader;
        }

        static final class Chunk {
            final int firstLineNumber;
            final List<String> lines;

            Chunk(int firstLineNumber, List<String> lines) {
                this.firstLineNumber = firstLineNumber;
                this.lines = lines;
            }
        }

        /** Returns the next lines to send, or null once the input has been used up. */
        synchronized Chunk next() throws IOException {
            if (exhausted) {
                return null;
            }
            final List<String> chunk = new ArrayList<>(CHUNK_SIZE);
            String line;
            while (chunk.size() < CHUNK_SIZE && (line = reader.readLine()) != null) {
                chunk.add(line);
            }
            if (chunk.isEmpty()) {
                exhausted = true;
                return null;
            }
            final Chunk result = new Chunk(nextLineNumber, chunk);
            nextLineNumber += chunk.size();
            return result;
        }
    }

    /**
     * input format json
     *
     * <p>{url: "http://test.com", key: "test.com"}
     *
     * <p>or plain text where each line is a URL and the other fields are left to their default
     * value i.e. no custom metadata, key determined by the server (i.e. hostname), no explicit
     * refetchable_from_date.
     *
     * <p>The input file can mix json and text lines.
     */
    private static URLItem parse(String input, String crawl) {
        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder = URLItem.newBuilder();
        if (input.trim().startsWith("{")) {
            try {
                JsonFormat.parser().merge(input, builder);
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        } else {
            String url = input.trim();
            URLInfo info = URLInfo.newBuilder().setUrl(url).setCrawlID(crawl).build();
            DiscoveredURLItem value = DiscoveredURLItem.newBuilder().setInfo(info).build();
            builder.setDiscovered(value);
            builder.setID(crawl + "_" + url);
        }
        return builder.build();
    }
}
