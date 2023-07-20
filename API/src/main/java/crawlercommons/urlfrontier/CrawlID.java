// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier;

public interface CrawlID {

    public String DEFAULT = "DEFAULT";

    public static String normaliseCrawlID(String crawlID) {
        if (crawlID.trim().isEmpty()) return DEFAULT;
        return crawlID;
    }
}
