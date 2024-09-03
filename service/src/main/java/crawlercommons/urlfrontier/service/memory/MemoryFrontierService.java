// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.memory;

import com.google.protobuf.InvalidProtocolBufferException;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLStatusRequest;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueInterface;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.SynchronizedStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a URL Frontier service using in memory data structures. Useful for
 * testing the API.
 */
public class MemoryFrontierService extends AbstractFrontierService {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(MemoryFrontierService.class);

    public MemoryFrontierService(final Map<String, String> configuration, String host, int port) {
        super(configuration, host, port);
    }

    // no explicit config
    public MemoryFrontierService(String host, int port) {
        this(new HashMap<String, String>(), host, port);
    }

    /** @return true if at least one URL has been sent for this queue, false otherwise */
    @Override
    protected int sendURLsForQueue(
            QueueInterface queue,
            QueueWithinCrawl prefixed_key,
            int maxURLsPerQueue,
            int secsUntilRequestable,
            long now,
            SynchronizedStreamObserver<URLInfo> responseObserver) {
        Iterator<InternalURL> iter = ((PriorityQueue<InternalURL>) queue).iterator();
        int alreadySent = 0;

        while (iter.hasNext() && alreadySent < maxURLsPerQueue) {
            InternalURL item = iter.next();

            // check that is is due
            if (item.nextFetchDate > now) {
                // they are sorted by date no need to go further
                return alreadySent;
            }

            // check that the URL is not already being processed
            if (item.heldUntil > now) {
                continue;
            }

            // this one is good to go
            try {
                // check that we haven't already reached the number of queues
                if (alreadySent == 0 && !responseObserver.tryTakingToken()) {
                    return 0;
                }

                responseObserver.onNext(item.toURLInfo(prefixed_key));

                // mark it as not processable for N secs
                item.heldUntil = now + secsUntilRequestable;

                alreadySent++;
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Caught unlikely error ", e);
            }
        }

        return alreadySent;
    }

    @Override
    protected AckMessage.Status putURLItem(URLItem value) {

        Object[] parsed = InternalURL.from(value);

        String key = (String) parsed[0];
        Boolean discovered = (Boolean) parsed[1];
        InternalURL iu = (InternalURL) parsed[2];

        putURLs_urls_count.inc();

        putURLs_discovered_count.labels(discovered.toString().toLowerCase()).inc();

        // has a queue key been defined? if not use the hostname
        if (key.equals("")) {
            LOG.debug("key missing for {}", iu.url);
            key = provideMissingKey(iu.url);
            if (key == null) {
                LOG.error("Malformed URL {}", iu.url);
                return Status.SKIPPED;
            }
        }

        // check that the key is not too long
        if (key.length() > 255) {
            LOG.error("Key too long: {}", key);
            return Status.SKIPPED;
        }

        QueueWithinCrawl qk = QueueWithinCrawl.get(key, iu.crawlID);

        // get the priority queue or create one
        synchronized (getQueues()) {
            URLQueue queue = (URLQueue) getQueues().get(qk);
            if (queue == null) {
                getQueues().put(qk, new URLQueue(iu));
                return Status.OK;
            }

            // check whether the URL already exists
            if (queue.contains(iu)) {
                if (discovered) {
                    putURLs_alreadyknown_count.inc();
                    // we already discovered it - so no need for it
                    return Status.SKIPPED;
                } else {
                    // overwrite the existing version
                    queue.remove(iu);
                }
            }

            // add the new item
            // unless it is an update and it's nextFetchDate is 0 == NEVER
            if (!discovered && iu.nextFetchDate == 0) {
                putURLs_completed_count.inc();
                queue.addToCompleted(iu.url);
            } else {
                queue.add(iu);
            }
        }
        return Status.OK;
    }

    @Override
    public void getURLStatus(URLStatusRequest request, StreamObserver<URLItem> responseObserver) {

        String crawlId = request.getCrawlID();
        String url = request.getUrl();
        String key = request.getKey();
        boolean found = false;

        // has a queue key been defined? if not use the hostname
        if (key == null || key.equals("")) {
            LOG.debug("key missing for {}", url);
            key = provideMissingKey(url);
            if (key == null) {
                LOG.error("Malformed URL {}", url);
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT.asRuntimeException());
                return;
            }
        }

        LOG.info("getURLStatus crawlId={} key={} url={}", crawlId, key, url);

        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, crawlId);
        URLQueue queue = (URLQueue) getQueues().get(qwc);
        if (queue == null) {
            LOG.error("Could not find queue for Crawl={}, queue={}", crawlId, key);
            responseObserver.onError(io.grpc.Status.NOT_FOUND.asRuntimeException());
            return;
        }

        URLInfo.Builder infoBuilder = URLInfo.newBuilder();
        URLInfo info = infoBuilder.setCrawlID(crawlId).setKey(key).setUrl(url).build();

        URLItem.Builder builder = URLItem.newBuilder();

        KnownURLItem.Builder knownBuilder = KnownURLItem.newBuilder();

        if (queue.isCompleted(url)) {
            knownBuilder.setInfo(info);
            knownBuilder.setRefetchableFromDate(0);
            builder.setKnown(knownBuilder.build());

            found = true;
            responseObserver.onNext(builder.build());
        } else {
            Iterator<InternalURL> iter = queue.iterator();

            while (iter.hasNext()) {
                InternalURL item = iter.next();

                if (url.equals(item.url)) {

                    try {
                        knownBuilder.setInfo(item.toURLInfo(qwc));
                        knownBuilder.setRefetchableFromDate(item.nextFetchDate);
                    } catch (InvalidProtocolBufferException e) {
                        LOG.error(e.getMessage(), e);
                        responseObserver.onError(
                                io.grpc.Status.fromThrowable(e).asRuntimeException());
                        return;
                    }

                    builder.setKnown(knownBuilder.build());
                    found = true;
                    responseObserver.onNext(builder.build());
                    break;
                }
            }
        }

        if (found) {
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(io.grpc.Status.NOT_FOUND.asRuntimeException());
        }
    }
}
