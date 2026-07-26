// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import picocli.CommandLine.IVersionProvider;

/**
 * Reports the version of the service as set in the POM. The properties file it reads is filtered by
 * Maven at build time, which avoids having to hardcode the version in the sources when releasing.
 */
public class VersionProvider implements IVersionProvider {

    static final String UNKNOWN_VERSION = "unknown";

    private static final String RESOURCE = "/crawlercommons/urlfrontier/service/version.properties";

    @Override
    public String[] getVersion() throws IOException {
        final Properties props = new Properties();
        try (InputStream is = VersionProvider.class.getResourceAsStream(RESOURCE)) {
            if (is == null) {
                return new String[] {UNKNOWN_VERSION};
            }
            props.load(is);
        }
        return new String[] {props.getProperty("version", UNKNOWN_VERSION)};
    }
}
