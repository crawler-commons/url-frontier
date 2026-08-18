// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/** Tests for ParamHelper parameter parsing */
class ParamHelperTest {

    // --- getIntegerParameter (OptionalInt return) ---

    @Test
    void getIntegerParameter_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "42");
        OptionalInt result = ParamHelper.getIntegerParameter(config, "test.int");
        assertTrue(result.isPresent());
        assertEquals(42, result.getAsInt());
    }

    @Test
    void getIntegerParameter_missingKey_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        assertTrue(ParamHelper.getIntegerParameter(config, "missing").isEmpty());
    }

    @Test
    void getIntegerParameter_emptyString_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "");
        assertTrue(ParamHelper.getIntegerParameter(config, "test.int").isEmpty());
    }

    @Test
    void getIntegerParameter_invalidValue_throwsException() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "not-a-number");
        assertThrows(
                IllegalArgumentException.class,
                () -> ParamHelper.getIntegerParameter(config, "test.int"));
    }

    // --- getIntegerParameter with default ---

    @Test
    void getIntegerParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(100, ParamHelper.getIntegerParameter(config, "missing", 100));
    }

    @Test
    void getIntegerParameter_withDefault_returnsParsedWhenSet() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "42");
        assertEquals(42, ParamHelper.getIntegerParameter(config, "test.int", 100));
    }

    @Test
    void getIntegerParameter_withDefault_returnsDefaultWhenEmpty() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "");
        assertEquals(50, ParamHelper.getIntegerParameter(config, "test.int", 50));
    }

    @Test
    void getIntegerParameter_withDefault_invalidValue_throwsException() {
        Map<String, String> config = new HashMap<>();
        config.put("test.int", "not-a-number");
        assertThrows(
                IllegalArgumentException.class,
                () -> ParamHelper.getIntegerParameter(config, "test.int", 10));
    }

    // --- getLongParameter (OptionalLong return) ---

    @Test
    void getLongParameter_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.long", "1234567890");
        OptionalLong result = ParamHelper.getLongParameter(config, "test.long");
        assertTrue(result.isPresent());
        assertEquals(1234567890L, result.getAsLong());
    }

    @Test
    void getLongParameter_missingKey_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        assertTrue(ParamHelper.getLongParameter(config, "missing").isEmpty());
    }

    @Test
    void getLongParameter_invalidValue_throwsException() {
        Map<String, String> config = new HashMap<>();
        config.put("test.long", "not-a-long");
        assertThrows(
                IllegalArgumentException.class,
                () -> ParamHelper.getLongParameter(config, "test.long"));
    }

    // --- getLongParameter with default ---

    @Test
    void getLongParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(999L, ParamHelper.getLongParameter(config, "missing", 999L));
    }

    @Test
    void getLongParameter_withDefault_returnsParsedWhenSet() {
        Map<String, String> config = new HashMap<>();
        config.put("test.long", "42");
        assertEquals(42L, ParamHelper.getLongParameter(config, "test.long", 999L));
    }

    // --- getDoubleParameter (OptionalDouble return) ---

    @Test
    void getDoubleParameter_noDefault_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "3.14");
        OptionalDouble result = ParamHelper.getDoubleParameter(config, "test.double");
        assertTrue(result.isPresent());
        assertEquals(3.14, result.getAsDouble(), 0.001);
    }

    @Test
    void getDoubleParameter_noDefault_missingKey_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        assertTrue(ParamHelper.getDoubleParameter(config, "missing").isEmpty());
    }

    @Test
    void getDoubleParameter_noDefault_emptyString_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "");
        assertTrue(ParamHelper.getDoubleParameter(config, "test.double").isEmpty());
    }

    @Test
    void getDoubleParameter_noDefault_invalidValue_throwsException() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "not-a-double");
        assertThrows(
                IllegalArgumentException.class,
                () -> ParamHelper.getDoubleParameter(config, "test.double"));
    }

    // --- getDoubleParameter with default ---

    @Test
    void getDoubleParameter_withValue_returnsParsedValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "3.14");
        assertEquals(3.14, ParamHelper.getDoubleParameter(config, "test.double", 0.0), 0.001);
    }

    @Test
    void getDoubleParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals(2.71, ParamHelper.getDoubleParameter(config, "missing", 2.71), 0.001);
    }

    @Test
    void getDoubleParameter_withDefault_returnsDefaultWhenEmpty() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "");
        assertEquals(2.71, ParamHelper.getDoubleParameter(config, "test.double", 2.71), 0.001);
    }

    @Test
    void getDoubleParameter_invalidValue_throwsException() {
        Map<String, String> config = new HashMap<>();
        config.put("test.double", "not-a-double");
        assertThrows(
                IllegalArgumentException.class,
                () -> ParamHelper.getDoubleParameter(config, "test.double", 0.0));
    }

    // --- getStringParameter ---

    @Test
    void getStringParameter_withValue_returnsValue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.str", "hello");
        assertEquals("hello", ParamHelper.getStringParameter(config, "test.str"));
    }

    @Test
    void getStringParameter_missingKey_returnsEmpty() {
        Map<String, String> config = new HashMap<>();
        assertEquals("", ParamHelper.getStringParameter(config, "missing"));
    }

    @Test
    void getStringParameter_withDefault_returnsDefaultWhenMissing() {
        Map<String, String> config = new HashMap<>();
        assertEquals("default", ParamHelper.getStringParameter(config, "missing", "default"));
    }

    @Test
    void getStringParameter_withDefault_returnsValueWhenSet() {
        Map<String, String> config = new HashMap<>();
        config.put("test.str", "hello");
        assertEquals("hello", ParamHelper.getStringParameter(config, "test.str", "default"));
    }

    @Test
    void getStringParameter_withDefault_returnsDefaultWhenEmpty() {
        Map<String, String> config = new HashMap<>();
        config.put("test.str", "");
        assertEquals("default", ParamHelper.getStringParameter(config, "test.str", "default"));
    }

    // --- getBooleanParameter ---

    @Test
    void getBooleanParameter_withTrueValue_returnsTrue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "true");
        assertTrue(ParamHelper.getBooleanParameter(config, "test.bool", false));
    }

    @Test
    void getBooleanParameter_withFalseValue_returnsFalse() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "false");
        assertFalse(ParamHelper.getBooleanParameter(config, "test.bool", true));
    }

    @Test
    void getBooleanParameter_emptyString_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "");
        assertFalse(ParamHelper.getBooleanParameter(config, "test.bool", false));
        assertTrue(ParamHelper.getBooleanParameter(config, "test.bool", true));
    }

    @Test
    void getBooleanParameter_nullValue_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", null);
        assertFalse(ParamHelper.getBooleanParameter(config, "test.bool", false));
        assertTrue(ParamHelper.getBooleanParameter(config, "test.bool", true));
    }

    @Test
    void getBooleanParameter_missingKey_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        assertTrue(ParamHelper.getBooleanParameter(config, "missing", true));
        assertFalse(ParamHelper.getBooleanParameter(config, "missing", false));
    }

    @Test
    void getBooleanParameter_nonBooleanString_returnsFalse() {
        Map<String, String> config = new HashMap<>();
        config.put("test.bool", "yes");
        assertFalse(ParamHelper.getBooleanParameter(config, "test.bool", false));
        config.put("test.bool", "1");
        assertFalse(ParamHelper.getBooleanParameter(config, "test.bool", false));
    }

    // --- getFlagParameter ---

    @Test
    void getFlagParameter_missingKey_returnsDefault() {
        Map<String, String> config = new HashMap<>();
        assertTrue(ParamHelper.getFlagParameter(config, "missing", true));
        assertFalse(ParamHelper.getFlagParameter(config, "missing", false));
    }

    @Test
    void getFlagParameter_emptyString_returnsTrue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.flag", "");
        assertTrue(ParamHelper.getFlagParameter(config, "test.flag", false));
    }

    @Test
    void getFlagParameter_nullValue_returnsTrue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.flag", null);
        assertTrue(ParamHelper.getFlagParameter(config, "test.flag", false));
    }

    @Test
    void getFlagParameter_trueValue_returnsTrue() {
        Map<String, String> config = new HashMap<>();
        config.put("test.flag", "true");
        assertTrue(ParamHelper.getFlagParameter(config, "test.flag", false));
    }

    @Test
    void getFlagParameter_falseValue_returnsFalse() {
        Map<String, String> config = new HashMap<>();
        config.put("test.flag", "false");
        assertFalse(ParamHelper.getFlagParameter(config, "test.flag", true));
    }
}
