// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import crawlercommons.urlfrontier.service.QueueInterface;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueMetadata implements QueueInterface {

    public QueueMetadata() {}

    /** number of URLs that are not scheduled anymore * */
    private AtomicInteger completed = new AtomicInteger(0);

    /** number of URLs scheduled in the queue * */
    private AtomicInteger active = new AtomicInteger(0);

    private Optional<Integer> limit = Optional.empty();

    private long blockedUntil = -1;

    private int delay = -1;

    private long lastProduced = 0;

    private Map<String, Long> beingProcessed = null;

    @Override
    public int getInProcess(long now) {
        synchronized (this) {
            if (beingProcessed == null) return 0;
            // check that the content of beingProcessed is still valid
            beingProcessed
                    .entrySet()
                    .removeIf(
                            e -> {
                                return e.getValue().longValue() <= now;
                            });
            return beingProcessed.size();
        }
    }

    public void holdUntil(String url, long timeinSec) {
        synchronized (this) {
            if (beingProcessed == null) beingProcessed = new LinkedHashMap<>();
            beingProcessed.put(url, timeinSec);
        }
    }

    public boolean isHeld(String url, long now) {
        synchronized (this) {
            if (beingProcessed == null) return false;
            Long timeout = beingProcessed.get(url);
            if (timeout != null) {
                if (timeout.longValue() < now) {
                    // release!
                    beingProcessed.remove(url);
                    return false;
                } else return true;
            }
            return false;
        }
    }

    public void removeFromProcessed(String url) {
        synchronized (this) {
            // should not happen
            if (beingProcessed == null) return;

            // remove from ephemeral cache of URLs in process
            beingProcessed.remove(url);
        }
    }

    @Override
    public int getCountCompleted() {
        return completed.get();
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
    public int getDelay() {
        return delay;
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

    public void decrementActive() {
        active.decrementAndGet();
    }

    public void incrementActive() {
        active.incrementAndGet();
    }

    public void setActiveCount(int value) {
        active.set(value);
    }

    public void setCompletedCount(int value) {
        completed.set(value);
    }

    public void incrementCompleted() {
        completed.incrementAndGet();
    }

    @Override
    public int countActive() {
        return active.get();
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
}
