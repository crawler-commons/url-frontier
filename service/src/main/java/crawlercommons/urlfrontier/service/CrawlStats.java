// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.LoggerFactory;

public class CrawlStats {

    private static final org.slf4j.Logger LOG =
            LoggerFactory.getLogger(MemoryFrontierService.class);

    // Nb of URLs per crawlID
    private final ConcurrentMap<String, MutableLong> urlCountMap = new ConcurrentHashMap<>();

    // Nb of completed URLs per crawlID
    private final ConcurrentMap<String, MutableLong> completedUrlCountMap =
            new ConcurrentHashMap<>();

    /**
     * Increment the total URL count for a given crawlID
     *
     * @param crawlID
     * @return
     */
    public long incTotalURLCount(String crawlId) {

        // Using compute to atomically initialize or increment
        MutableLong res =
                urlCountMap.compute(
                        crawlId,
                        (k, v) -> {
                            if (v == null) {
                                return new MutableLong(1);
                            } else {
                                v.increment();
                                return v;
                            }
                        });

        return res.longValue();
    }

    /**
     * Increment the completed URL count for a given crawlID
     *
     * @param crawlID
     * @return
     */
    public long incCompletedURLCount(String crawlId) {

        // Using compute to atomically initialize or increment
        MutableLong res =
                completedUrlCountMap.compute(
                        crawlId,
                        (k, v) -> {
                            if (v == null) {
                                return new MutableLong(1);
                            } else {
                                v.increment();
                                return v;
                            }
                        });

        return res.longValue();
    }

    /**
     * Get the total URL count for a given crawlID
     *
     * @param crawlID
     * @return
     */
    public long getTotalURLCount(String crawlID) {
        MutableLong count = urlCountMap.get(crawlID);
        if (count != null) {
            return count.longValue();
        } else {
            return 0;
        }
    }

    /**
     * Get the total URL count for a given crawlID
     *
     * @param crawlID
     * @return
     */
    public long getCompletedURLCount(String crawlID) {
        MutableLong count = completedUrlCountMap.get(crawlID);
        if (count != null) {
            return count.longValue();
        } else {
            return 0;
        }
    }

    /**
     * Delete stats for a given crawlID
     *
     * @param crawlID
     */
    public void delete(String crawlID) {
        urlCountMap.remove(crawlID);
        completedUrlCountMap.remove(crawlID);
    }

    /** Log stats for all crawlIDs */
    public void logStats() {
        for (String crawlID : urlCountMap.keySet()) {
            long total = getTotalURLCount(crawlID);
            long completed = getCompletedURLCount(crawlID);
            LOG.info("CrawlID: {}, Total URLs: {}, Completed URLs: {}", crawlID, total, completed);
        }
    }
}
