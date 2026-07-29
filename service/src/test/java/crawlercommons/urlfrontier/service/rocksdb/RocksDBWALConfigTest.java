// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks how the rocksdb.wal.disable configuration key is interpreted */
class RocksDBWALConfigTest {

    @Test
    void enabledWhenNotConfigured() {
        assertFalse(RocksDBService.disableWAL(new HashMap<>()));
    }

    /** the mere presence of the key counts as a flag */
    @Test
    void disabledWhenKeyHasNoValue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.wal.disable", null);
        assertTrue(RocksDBService.disableWAL(conf));

        conf.put("rocksdb.wal.disable", "");
        assertTrue(RocksDBService.disableWAL(conf));
    }

    @Test
    void disabledWhenSetToTrue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.wal.disable", "true");
        assertTrue(RocksDBService.disableWAL(conf));
    }

    @Test
    void enabledWhenSetToFalse() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.wal.disable", "false");
        assertFalse(RocksDBService.disableWAL(conf));
    }
}
