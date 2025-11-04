// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import java.time.Instant;
import org.slf4j.LoggerFactory;

public class ServiceTestUtil {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ServiceTestUtil.class);

    public static void initURLs(AbstractFrontierService service) {

        String crawlId = "crawl_id";
        String url1 = "https://www.mysite.com/discovered";
        String key1 = "queue_mysite";
        String url2 = "https://www.mysite.com/completed";
        String key2 = "queue_mysite";
        String url3 = "https://www.mysite.com/knowntorefetch";
        String key3 = "queue_mysite";
        String url4 = "https://www.mysite.com/secondqueue";
        String key4 = "another_queue";

        int sent = 0;

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder1 = URLItem.newBuilder();
        StringList sl1 = StringList.newBuilder().addValues("md1").build();
        StringList sl2 = StringList.newBuilder().addValues("md2").build();
        StringList sl3 = StringList.newBuilder().addValues("md3").build();
        StringList sl4 = StringList.newBuilder().addValues("md4").build();

        // Adding a discovered URL
        URLInfo info1 =
                URLInfo.newBuilder()
                        .setUrl(url1)
                        .setCrawlID(crawlId)
                        .setKey(key1)
                        .putMetadata("meta1", sl1)
                        .build();

        DiscoveredURLItem disco1 = DiscoveredURLItem.newBuilder().setInfo(info1).build();
        builder1.setDiscovered(disco1);
        builder1.setID(crawlId + "_" + url1);

        service.putURLItem(builder1.build());
        sent++;

        // Adding a completed URL
        URLInfo info2 =
                URLInfo.newBuilder()
                        .setUrl(url2)
                        .setCrawlID(crawlId)
                        .setKey(key2)
                        .putMetadata("meta2", sl2)
                        .build();

        DiscoveredURLItem disco2 = DiscoveredURLItem.newBuilder().setInfo(info2).build();
        builder1.clear();
        builder1.setDiscovered(disco2);
        builder1.setID(crawlId + "_" + url2);

        // Add url2 first as discovered
        service.putURLItem(builder1.build());
        sent++;

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder2 = URLItem.newBuilder();
        KnownURLItem known =
                KnownURLItem.newBuilder().setInfo(info2).setRefetchableFromDate(0).build();

        builder2.setKnown(known);
        builder2.setID(crawlId + "_" + url2);

        // Then resend it with a refetch date of 0
        service.putURLItem(builder2.build());
        sent++;

        // Adding a known URL to refetch
        URLInfo info3 =
                URLInfo.newBuilder()
                        .setUrl(url3)
                        .setCrawlID(crawlId)
                        .setKey(key3)
                        .putMetadata("meta3", sl3)
                        .build();

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder3 = URLItem.newBuilder();
        KnownURLItem torefetch =
                KnownURLItem.newBuilder()
                        .setInfo(info3)
                        .setRefetchableFromDate(Instant.now().getEpochSecond() + 3600)
                        .build();

        builder3.setKnown(torefetch);
        builder3.setID(crawlId + "_" + url3);

        service.putURLItem(builder3.build());
        sent++;

        // Adding a known URL to refetch in another queue
        URLInfo info4 =
                URLInfo.newBuilder()
                        .setUrl(url4)
                        .setCrawlID(crawlId)
                        .setKey(key4)
                        .putMetadata("meta4", sl4)
                        .build();

        crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder4 = URLItem.newBuilder();
        KnownURLItem queuetwo =
                KnownURLItem.newBuilder()
                        .setInfo(info4)
                        .setRefetchableFromDate(Instant.now().getEpochSecond() + 3600)
                        .build();

        builder4.setKnown(queuetwo);
        builder4.setID(crawlId + "_" + url4);

        service.putURLItem(builder4.build());
        sent++;

        LOG.info("Init test data with {} putURLItem calls", sent);
    }
}
