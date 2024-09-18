// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.memory;

import crawlercommons.urlfrontier.service.QueueInterface;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public class URLQueue extends PriorityQueue<InternalURL> implements QueueInterface {

    public URLQueue(InternalURL initial) {
        this.add(initial);
    }

    // keep a hash of the completed URLs
    // these won't be refetched
    private HashSet<String> completed = new HashSet<>();

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

    /** @return The unmodifiable set of completed URLs */
    public Set<String> getCompleted() {
        return Collections.unmodifiableSet(completed);
    }
}
