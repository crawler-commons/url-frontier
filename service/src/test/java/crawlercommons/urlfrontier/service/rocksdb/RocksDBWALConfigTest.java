// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.rocksdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.service.ParamHelper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks how the rocksdb.wal.disable configuration key is interpreted */
class RocksDBWALConfigTest {

    @Test
    void enabledWhenNotConfigured() {
        boolean walDisabled =
                ParamHelper.getBooleanParameter(new HashMap<>(), "rocksdb.wal.disable", false);

        assertFalse(walDisabled);
    }

    /** the mere presence of the key counts as a flag */
    @Test
    void disabledWhenKeyHasNoValue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.wal.disable", null);
        boolean walDisabled = ParamHelper.getBooleanParameter(conf, "rocksdb.wal.disable", false);
        assertTrue(walDisabled);

        conf.put("rocksdb.wal.disable", "");
        walDisabled = ParamHelper.getBooleanParameter(conf, "rocksdb.wal.disable", false);
        assertTrue(walDisabled);
    }

    @Test
    void disabledWhenSetToTrue() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.wal.disable", "true");
        boolean walDisabled = ParamHelper.getBooleanParameter(conf, "rocksdb.wal.disable", false);
        assertTrue(walDisabled);
    }

    @Test
    void enabledWhenSetToFalse() {
        final Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.wal.disable", "false");
        boolean walDisabled = ParamHelper.getBooleanParameter(conf, "rocksdb.wal.disable", false);
        assertFalse(walDisabled);
    }
}
