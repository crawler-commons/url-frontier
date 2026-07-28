// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks how the rocksdb.bloom.filters configuration key is interpreted */
class RocksDBBloomFilterConfigTest {

    @Test
    void enabledWhenNotConfigured() {
        assertTrue(RocksDBService.useBloomFilters(new HashMap<>()));
    }

    /** the key used to be a flag without a value */
    @Test
    void enabledWhenKeyHasNoValue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.bloom.filters", null);
        assertTrue(RocksDBService.useBloomFilters(conf));

        conf.put("rocksdb.bloom.filters", "");
        assertTrue(RocksDBService.useBloomFilters(conf));
    }

    @Test
    void enabledWhenSetToTrue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.bloom.filters", "true");
        assertTrue(RocksDBService.useBloomFilters(conf));
    }

    @Test
    void disabledWhenSetToFalse() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.bloom.filters", "false");
        assertFalse(RocksDBService.useBloomFilters(conf));
    }
}
