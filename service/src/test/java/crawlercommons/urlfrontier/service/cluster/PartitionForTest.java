// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionForTest {

    private static final List<String> THREE_NODES =
            List.of("node1:7071", "node2:7071", "node3:7071");

    @Test
    void matchesLegacyPutUrlsFormula() {
        QueueWithinCrawl qwc = QueueWithinCrawl.get("example.com", "DEFAULT");
        assertEquals(
                Math.abs(qwc.toString().hashCode() % THREE_NODES.size()),
                DistributedFrontierService.partitionFor(qwc, THREE_NODES));
    }

    @Test
    void negativeHashKeepsLegacyMapping() {
        // with 2 nodes abs and floorMod coincide; 3 nodes tell them apart:
        // abs(-2 % 3) = 2 while floorMod(-2, 3) = 1
        QueueWithinCrawl negative = null;
        for (int i = 0; i < 10_000; i++) {
            QueueWithinCrawl candidate = QueueWithinCrawl.get("host-" + i + ".com", "DEFAULT");
            if (candidate.toString().hashCode() % THREE_NODES.size() < 0) {
                negative = candidate;
                break;
            }
        }
        assertNotNull(negative, "no negative-hash key found in search space");
        int hash = negative.toString().hashCode();
        assertEquals(
                Math.abs(hash % 3), DistributedFrontierService.partitionFor(negative, THREE_NODES));
        assertNotEquals(
                Math.floorMod(hash, 3),
                DistributedFrontierService.partitionFor(negative, THREE_NODES));
    }
}
