// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.memory;

import crawlercommons.urlfrontier.service.QueueInterface;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class URLQueue extends PriorityQueue<InternalURL> implements QueueInterface {

    public URLQueue(InternalURL initial) {
        this.add(initial);
        setCreationDateIfAbsent(initial.url, Instant.now().getEpochSecond());
    }

    // keep a hash of the completed URLs
    // these won't be refetched
    // written by the putURLs worker threads, read by the getURLs gate via isLimitReached
    private Set<String> completed = ConcurrentHashMap.newKeySet();

    // creation date of every URL held by this queue, active or completed
    // scoped to the queue so that the entries go away with it and so that the same URL in
    // another queue or crawl keeps its own date
    // written by the putURLs worker threads, read without synchronization by getURLStatus
    // and the URL iterators
    private final Map<String, Long> creationDates = new ConcurrentHashMap<>();

    private Optional<Integer> limit = Optional.empty();

    private long blockedUntil = -1;

    private int delay = -1;

    private long lastProduced = 0;

    @Override
    public int getInProcess(long now) {
        // a URL in process has a heldUntil and is at the beginning of a queue
        Iterator<InternalURL> iter = this.iterator();
        int inproc = 0;
        while (iter.hasNext()) {
            InternalURL iu = iter.next();
            if (iu.heldUntil > now) inproc++;
            // can stop if no heldUntil at all
            else if (iu.heldUntil == -1) return inproc;
        }
        return inproc;
    }

    @Override
    public boolean contains(Object iu) {
        // been fetched before?
        if (completed.contains(((InternalURL) iu).url)) {
            return true;
        }
        return super.contains(iu);
    }

    public void addToCompleted(String url) {
        completed.add(url);
    }

    /**
     * Records when a URL was first added to this queue. Later versions of the same URL keep the
     * date of the first one, like the RocksDB backend does.
     */
    public void setCreationDateIfAbsent(String url, long epochSeconds) {
        creationDates.putIfAbsent(url, epochSeconds);
    }

    /**
     * @return the epoch seconds at which the URL was added to this queue, 0 if unknown
     */
    public long getCreationDate(String url) {
        return creationDates.getOrDefault(url, 0L);
    }

    public void removeCreationDate(String url) {
        creationDates.remove(url);
    }

    @Override
    public int getCountCompleted() {
        return completed.size();
    }

    @Override
    public void setBlockedUntil(long until) {
        blockedUntil = until;
    }

    @Override
    public long getBlockedUntil() {
        return blockedUntil;
    }

    @Override
    public void setDelay(int delayRequestable) {
        this.delay = delayRequestable;
    }

    @Override
    public long getLastProduced() {
        return lastProduced;
    }

    @Override
    public void setLastProduced(long lastProduced) {
        this.lastProduced = lastProduced;
    }

    @Override
    public int getDelay() {
        return delay;
    }

    @Override
    public int countActive() {
        return this.size();
    }

    public boolean isCompleted(String url) {
        return completed.contains(url);
    }

    @Override
    public void setCrawlLimit(int crawlLimit) {
        if (crawlLimit == 0) {
            limit = Optional.empty();
        } else {
            limit = Optional.of(crawlLimit);
        }
    }

    @Override
    public Boolean isLimitReached() {
        if (limit.isEmpty()) {
            return false;
        }

        return getCountCompleted() >= limit.get();
    }

    /**
     * @return The set of completed URLs
     */
    public Set<String> getCompleted() {
        return completed;
    }
}
