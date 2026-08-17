// SPDX-FileCopyrightText: 2025 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.util.Map;
import java.util.Optional;
import org.slf4j.LoggerFactory;

/** Utility functions to retrieve and parse configuration parameters */
public class ParamHelper {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ParamHelper.class);

    /**
     * Retrieves and parses a configuration parameter using the provided parser function.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal An optional default value if the parameter is not set.
     * @param parser Function to parse the string value to the target type.
     * @param fallback Value to use if parameter is not set and no default provided.
     * @param <T> The target type.
     * @return The parsed parameter value, default value, or fallback.
     */
    private static <T> T parseParameter(
            Map<String, String> config,
            String paramName,
            Optional<T> defaultVal,
            java.util.function.Function<String, T> parser,
            T fallback) {

        String stringVal = config.get(paramName);

        // defaultValue could potentially contain null but the public methods
        // pass primitive types (int, long, double) or empty String so should not happen.
        T val = (defaultVal != null) ? defaultVal.orElse(fallback) : fallback;
        if (stringVal != null && !stringVal.isEmpty()) {
            try {
                val = parser.apply(stringVal);
            } catch (NumberFormatException e) {
                LOG.error(
                        "Error parsing {} for config parameter {}",
                        parser.getClass().getSimpleName(),
                        paramName);
                System.exit(-1);
            }
        }

        return val;
    }

    /**
     * Retrieves an integer configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal An optional default value if the parameter is not set.
     * @return The integer parameter value if defined and parsable; otherwise the default value or
     *     -1. Exits the program if the parameter value cannot be parsed as an integer.
     */
    public static int getIntegerParameter(
            Map<String, String> config, String paramName, Optional<Integer> defaultVal) {
        return parseParameter(config, paramName, defaultVal, Integer::parseInt, -1);
    }

    /**
     * Retrieves a long configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal An optional default value if the parameter is not set.
     * @return The long parameter value if defined and parsable; otherwise the default value or -1.
     *     Exits the program if the parameter value cannot be parsed as a long.
     */
    public static long getLongParameter(
            Map<String, String> config, String paramName, Optional<Long> defaultVal) {
        return parseParameter(config, paramName, defaultVal, Long::parseLong, -1L);
    }

    /**
     * Retrieves a double configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal An optional default value if the parameter is not set.
     * @return The double parameter value if defined and parsable; otherwise the default value or
     *     NaN. Exits the program if the parameter value cannot be parsed as a double.
     */
    public static double getDoubleParameter(
            Map<String, String> config, String paramName, Optional<Double> defaultVal) {
        return parseParameter(config, paramName, defaultVal, Double::parseDouble, Double.NaN);
    }

    /**
     * Retrieves a string configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal An optional default value if the parameter is not set.
     * @return The string parameter value if defined and non-empty; otherwise the default value if
     *     provided (empty string if default is empty), or empty string if no default provided.
     */
    public static String getStringParameter(
            Map<String, String> config, String paramName, Optional<String> defaultVal) {

        return parseParameter(config, paramName, defaultVal, s -> s, "");
    }

    /**
     * Retrieves a boolean configuration parameter from the map.
     *
     * @param config The configuration map holding parameter names and values.
     * @param paramName The name of the configuration parameter to retrieve.
     * @param defaultVal The default value if the parameter is not set.
     * @return The boolean parameter value if defined; otherwise the default value. Empty string or
     *     null values are treated as true.
     */
    public static boolean getBooleanParameter(
            final Map<String, String> config, String paramName, boolean defaultVal) {
        if (!config.containsKey(paramName)) {
            return defaultVal;
        } else {
            String value = config.get(paramName);
            return value == null || value.isEmpty() || Boolean.parseBoolean(value);
        }
    }
}
