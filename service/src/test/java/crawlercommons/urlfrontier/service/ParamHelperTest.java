// SPDX-FileCopyrightText: 2025 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests for ParamHelper parameter parsing */
class ParamHelperTest {

    @Test
    void getIntegerParameter_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "42");
        assertEquals(42, ParamHelper.getIntegerParameter(config, "test.int", Optional.empty()));
    }

    @Test
    void getIntegerParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(100, ParamHelper.getIntegerParameter(config, "missing", Optional.of(100)));
    }

    @Test
    void getIntegerParameter_noDefault_returnsFallbackWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(-1, ParamHelper.getIntegerParameter(config, "missing", Optional.empty()));
    }

    @Test
    void getIntegerParameter_emptyString_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "");
        assertEquals(50, ParamHelper.getIntegerParameter(config, "test.int", Optional.of(50)));
    }

    @Test
    void getIntegerParameter_invalidValue_exitsProgram() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "not-a-number");
        // System.exit is called, so we can't easily test this without mocking
        // This test documents the behavior
    }

    @Test
    void getLongParameter_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.long", "1234567890");
        assertEquals(1234567890L, ParamHelper.getLongParameter(config, "test.long", Optional.empty()));
    }

    @Test
    void getLongParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(999L, ParamHelper.getLongParameter(config, "missing", Optional.of(999L)));
    }

    @Test
    void getLongParameter_noDefault_returnsFallbackWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(-1L, ParamHelper.getLongParameter(config, "missing", Optional.empty()));
    }

    @Test
    void getDoubleParameter_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "3.14");
        assertEquals(3.14, ParamHelper.getDoubleParameter(config, "test.double", Optional.empty()));
    }

    @Test
    void getDoubleParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(2.71, ParamHelper.getDoubleParameter(config, "missing", Optional.of(2.71)));
    }

    @Test
    void getDoubleParameter_noDefault_returnsNaNWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertTrue(Double.isNaN(ParamHelper.getDoubleParameter(config, "missing", Optional.empty())));
    }

    @Test
    void getStringParameter_withValue_returnsValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.str", "hello");
        assertEquals("hello", ParamHelper.getStringParameter(config, "test.str", Optional.empty()));
    }

    @Test
    void getStringParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals("default", ParamHelper.getStringParameter(config, "missing", Optional.of("default")));
    }

    @Test
    void getStringParameter_noDefault_returnsEmptyWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals("", ParamHelper.getStringParameter(config, "missing", Optional.empty()));
    }

    @Test
    void getStringParameter_nullDefault_returnsEmptyWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals("", ParamHelper.getStringParameter(config, "missing", null));
    }

    @Test
    void getStringParameter_emptyStringInConfig_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        config.put("test.str", "");
        assertEquals("default", ParamHelper.getStringParameter(config, "test.str", Optional.of("default")));
    }

    @Test
    void getStringParameter_optionalWithNull_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        assertEquals("", ParamHelper.getStringParameter(config, "missing", Optional.ofNullable(null)));
    }

    @Test
    void getBooleanParameter_withTrueValue_returnsTrue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "true");
        assertEquals(true, ParamHelper.getBooleanParameter(config, "test.bool", false));
    }

    @Test
    void getBooleanParameter_withFalseValue_returnsFalse() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "false");
        assertEquals(false, ParamHelper.getBooleanParameter(config, "test.bool", true));
    }

    @Test
    void getBooleanParameter_emptyString_returnsTrue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "");
        assertEquals(true, ParamHelper.getBooleanParameter(config, "test.bool", false));
    }
    
    @Test
    void getBooleanParameter_nullValue_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", null);
        assertEquals(true, ParamHelper.getBooleanParameter(config, "test.bool", true));
    }

    @Test
    void getBooleanParameter_missingKey_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        assertEquals(true, ParamHelper.getBooleanParameter(config, "missing", true));
        assertEquals(false, ParamHelper.getBooleanParameter(config, "missing", false));
    }

    @Test
    void getBooleanParameter_nonBooleanString_returnsFalse() {
        // Boolean.parseBoolean returns true for any non-null, non-"true" string
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "yes");
        assertEquals(false, ParamHelper.getBooleanParameter(config, "test.bool", false));
        config.put("test.bool", "1");
        assertEquals(false, ParamHelper.getBooleanParameter(config, "test.bool", false));
    }
}