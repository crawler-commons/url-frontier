// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for URLFrontierServer.addToConfig -- config line parsing */
class URLFrontierServerConfigTest {

    @Test
    void bareKeyStoredAsEmptyString() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "rocksdb.purge");

        assertTrue(config.containsKey("rocksdb.purge"));
        assertEquals("", config.get("rocksdb.purge"));
    }

    @Test
    void keyWithValue() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "read.thread.num=8");
        assertEquals(8, ParamHelper.getIntegerParameter(config, "read.thread.num", 1234));
    }

    @Test
    void keyWithEmptyValue() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "key=");
        assertEquals("", ParamHelper.getStringParameter(config, "key"));
    }

    @Test
    void valueContainingEquals() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "key=a=b");

        assertEquals("a=b", ParamHelper.getStringParameter(config, "key"));
    }

    @Test
    void keyAndValueWithSpacesTrimmed() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "  key  =  value  ");
        assertEquals("value", ParamHelper.getStringParameter(config, "key"));
    }

    @Test
    void bareKeyWithSpacesTrimmed() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "  rocksdb.purge  ");
        assertTrue(config.containsKey("rocksdb.purge"));
        assertEquals("", config.get("rocksdb.purge"));
    }

    @Test
    void emptyLineIgnored() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "");
        URLFrontierServer.addToConfig(config, "   ");
        assertTrue(config.isEmpty());
    }

    @Test
    void nullLineIgnored() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, null);
        assertTrue(config.isEmpty());
    }

    @Test
    void containsKeyWorksForFlagStyleKeys() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "rocksdb.purge");
        URLFrontierServer.addToConfig(config, "rocksdb.recovery.check");
        URLFrontierServer.addToConfig(config, "rocksdb.bloom.filters");
        assertTrue(config.containsKey("rocksdb.purge"));
        assertTrue(config.containsKey("rocksdb.recovery.check"));
        assertTrue(config.containsKey("rocksdb.bloom.filters"));
        assertTrue(ParamHelper.getFlagParameter(config, "rocksdb.bloom.filters", false));
    }

    @Test
    void getOrDefaultDoesNotReturnNullForBareKeys() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "implementation");
        // Before fix: getOrDefault returned null, breaking Class.forName
        // After fix: getOrDefault returns "" (not null)
        String result = config.getOrDefault("implementation", "default");
        assertNotNull(result);
        assertEquals("", result);
        assertEquals("", ParamHelper.getStringParameter(config, "implementation"));
    }

    @Test
    void getOrDefaultReturnsDefaultForAbsentKeys() {
        Map<String, String> config = new HashMap<>();
        String result = config.getOrDefault("absent.key", "default");
        assertEquals("default", result);
        assertEquals("default", ParamHelper.getStringParameter(config, "absent.key", "default"));
    }

    @Test
    void multipleKeysCoexist() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "rocksdb.purge");
        URLFrontierServer.addToConfig(config, "read.thread.num=4");
        URLFrontierServer.addToConfig(config, "rocksdb.path=/data");
        assertEquals("", config.get("rocksdb.purge"));
        assertEquals("4", config.get("read.thread.num"));
        assertEquals("/data", config.get("rocksdb.path"));
    }

    @Test
    void positionalArgOverride() {
        Map<String, String> config = new HashMap<>();
        URLFrontierServer.addToConfig(config, "read.thread.num=2");
        // Simulate positional arg override
        URLFrontierServer.addToConfig(config, "read.thread.num=8");
        assertEquals("8", config.get("read.thread.num"));
        assertEquals(8, ParamHelper.getIntegerParameter(config, "read.thread.num", 0));
    }
}
