// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The date part of the scheduling keys must always be 10 digits so that the keys of a queue sort
 * chronologically
 */
class PaddedDateTest {

    @Test
    void padsShortValues() {
        assertEquals("0000000000", RocksDBService.paddedDate(0));
        assertEquals("0000000042", RocksDBService.paddedDate(42));
        assertEquals("1753747200", RocksDBService.paddedDate(1753747200L));
    }

    @Test
    void leavesLongValuesUntouched() {
        // epoch seconds reach 11 digits in the year 2286
        assertEquals("10000000000", RocksDBService.paddedDate(10000000000L));
    }

    @Test
    void sortsChronologically() {
        assertTrue(
                RocksDBService.paddedDate(999).compareTo(RocksDBService.paddedDate(1753747200L))
                        < 0);
    }
}
