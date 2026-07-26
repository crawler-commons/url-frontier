// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** Guards against a broken resource filtering configuration in the POM. */
class VersionProviderTest {

    @Test
    void versionIsResolvedFromTheProject() throws Exception {
        final String[] version = new VersionProvider().getVersion();
        assertEquals(1, version.length);
        assertNotEquals(VersionProvider.UNKNOWN_VERSION, version[0]);
        assertFalse(version[0].contains("${"), "version was not filtered: " + version[0]);
    }
}
