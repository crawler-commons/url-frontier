// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Utility functions to retrieve and parse configuration parameters. */
public final class ParamHelper {

    private ParamHelper() {
        // utility class
    }

    /**
     * Retrieves an integer configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @return An OptionalInt containing the parsed value if the parameter is set and non-empty;
     *     empty otherwise. Throws IllegalArgumentException if the value cannot be parsed.
     */
    public static OptionalInt getIntegerParameter(Map<String, String> config, String paramName) {
        String stringVal = config.get(paramName);
        if (stringVal == null || stringVal.isEmpty()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(stringVal));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot parse value '" + stringVal + "' for config parameter " + paramName, e);
        }
    }

    /**
     * Retrieves an integer configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not set or empty.
     * @return The integer parameter value if defined and parsable; otherwise the default value.
     * @throws IllegalArgumentException if the parameter value cannot be parsed as an integer.
     */
    public static int getIntegerParameter(
            Map<String, String> config, String paramName, int defaultVal) {
        return getIntegerParameter(config, paramName).orElse(defaultVal);
    }

    /**
     * Retrieves a long configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @return An OptionalLong containing the parsed value if the parameter is set and non-empty;
     *     empty otherwise. Throws IllegalArgumentException if the value cannot be parsed.
     */
    public static OptionalLong getLongParameter(Map<String, String> config, String paramName) {
        String stringVal = config.get(paramName);
        if (stringVal == null || stringVal.isEmpty()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(stringVal));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot parse value '" + stringVal + "' for config parameter " + paramName, e);
        }
    }

    /**
     * Retrieves a long configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not set or empty.
     * @return The long parameter value if defined and parsable; otherwise the default value.
     * @throws IllegalArgumentException if the parameter value cannot be parsed as a long.
     */
    public static long getLongParameter(
            Map<String, String> config, String paramName, long defaultVal) {
        return getLongParameter(config, paramName).orElse(defaultVal);
    }

    /**
     * Retrieves a double configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @return An OptionalDouble containing the parsed value if the parameter is set and non-empty;
     *     empty otherwise. Throws IllegalArgumentException if the value cannot be parsed.
     */
    public static OptionalDouble getDoubleParameter(Map<String, String> config, String paramName) {
        String stringVal = config.get(paramName);
        if (stringVal == null || stringVal.isEmpty()) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(stringVal));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot parse value '" + stringVal + "' for config parameter " + paramName, e);
        }
    }

    /**
     * Retrieves a double configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not set or empty.
     * @return The double parameter value if defined and parsable; otherwise the default value.
     * @throws IllegalArgumentException if the parameter value cannot be parsed as a double.
     */
    public static double getDoubleParameter(
            Map<String, String> config, String paramName, double defaultVal) {
        String stringVal = config.get(paramName);
        if (stringVal == null || stringVal.isEmpty()) {
            return defaultVal;
        }
        try {
            return Double.parseDouble(stringVal);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cannot parse value '" + stringVal + "' for config parameter " + paramName, e);
        }
    }

    /**
     * Retrieves a string configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @return The string parameter value if defined and non-empty; otherwise empty string.
     */
    public static String getStringParameter(Map<String, String> config, String paramName) {
        String stringVal = config.get(paramName);
        return (stringVal != null && !stringVal.isEmpty()) ? stringVal : "";
    }

    /**
     * Retrieves a string configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not set or empty.
     * @return The string parameter value if defined and non-empty; otherwise the default value.
     */
    public static String getStringParameter(
            Map<String, String> config, String paramName, String defaultVal) {
        String stringVal = config.get(paramName);
        return (stringVal != null && !stringVal.isEmpty()) ? stringVal : defaultVal;
    }

    /**
     * Retrieves a boolean configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not set, null, or empty.
     * @return The boolean parameter value if defined and non-empty; otherwise the default value.
     */
    public static boolean getBooleanParameter(
            final Map<String, String> config, String paramName, boolean defaultVal) {
        if (!config.containsKey(paramName)) {
            return defaultVal;
        }
        String value = config.get(paramName);
        if (value == null || value.isEmpty()) {
            return defaultVal;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Retrieves a flag-style configuration parameter from the map. Flag-style keys are those where
     * the mere presence of the key (with no value or an empty value) means "true". Used for legacy
     * config keys like {@code rocksdb.bloom.filters} and {@code rocksdb.wal.disable} which
     * historically were set as bare keys without a value.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not present.
     * @return {@code true} if the key is present (even with null or empty value), or if the value
     *     is "true"; otherwise the result of {@link Boolean#parseBoolean(String)} or the default.
     */
    public static boolean getFlagParameter(
            final Map<String, String> config, String paramName, boolean defaultVal) {
        if (!config.containsKey(paramName)) {
            return defaultVal;
        }
        String value = config.get(paramName);
        return value == null || value.isEmpty() || Boolean.parseBoolean(value);
    }
}
