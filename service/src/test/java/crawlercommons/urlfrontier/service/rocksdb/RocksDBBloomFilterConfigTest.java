// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.service.ParamHelper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks how the rocksdb.bloom.filters configuration key is interpreted */
class RocksDBBloomFilterConfigTest {

    @Test
    void enabledWhenNotConfigured() {
        boolean bloomFilters =
                ParamHelper.getBooleanParameter(new HashMap<>(), "rocksdb.bloom.filters", true);
        assertTrue(bloomFilters);
    }

    /** the key used to be a flag without a value */
    @Test
    void enabledWhenKeyHasNoValue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.bloom.filters", null);
        boolean bloomFilters = ParamHelper.getBooleanParameter(conf, "rocksdb.bloom.filters", true);

        assertTrue(bloomFilters);

        conf.put("rocksdb.bloom.filters", "");
        bloomFilters = ParamHelper.getBooleanParameter(conf, "rocksdb.bloom.filters", true);

        assertTrue(bloomFilters);
    }

    @Test
    void enabledWhenSetToTrue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.bloom.filters", "true");
        boolean bloomFilters = ParamHelper.getBooleanParameter(conf, "rocksdb.bloom.filters", true);

        assertTrue(bloomFilters);
    }

    @Test
    void disabledWhenSetToFalse() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.bloom.filters", "false");
        boolean bloomFilters = ParamHelper.getBooleanParameter(conf, "rocksdb.bloom.filters", true);

        assertFalse(bloomFilters);
    }
}
