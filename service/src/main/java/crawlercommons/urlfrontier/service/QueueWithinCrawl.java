/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons SPDX-License-Identifier: Apache-2.0 Licensed to
 * Crawler-Commons under one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership. DigitalPebble licenses
 * this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package crawlercommons.urlfrontier.service;

import crawlercommons.urlfrontier.CrawlID;

public class QueueWithinCrawl implements Comparable<QueueWithinCrawl> {

    private String queue = "";
    private String crawlid = "";

    public static QueueWithinCrawl get(String queue, String crawlid) {
        return new QueueWithinCrawl(queue, CrawlID.normaliseCrawlID(crawlid));
    }

    public QueueWithinCrawl(String queue, String crawlid) {
        super();
        this.queue = queue;
        this.crawlid = crawlid;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getCrawlid() {
        return crawlid;
    }

    public void setCrawlid(String crawlid) {
        this.crawlid = crawlid;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof QueueWithinCrawl == false) return false;
        return equals((QueueWithinCrawl) arg0);
    }

    public boolean equals(QueueWithinCrawl qwc) {
        return this.getCrawlid().equals(qwc.getCrawlid()) && this.getQueue().equals(qwc.getQueue());
    }

    public boolean equals(String crawlID, String queueID) {
        return this.getCrawlid().equals(crawlID) && this.getQueue().equals(queueID);
    }

    @Override
    public int hashCode() {
        return this.getCrawlid().hashCode() + this.getQueue().hashCode();
    }

    @Override
    public String toString() {
        return this.getCrawlid() + "_" + this.getQueue();
    }

    public int compareTo(QueueWithinCrawl target) {
        int diff = this.crawlid.compareTo(target.crawlid);
        if (diff != 0) return diff;
        return this.queue.compareTo(target.queue);
    }

    public static final QueueWithinCrawl parseAndDeNormalise(final String currentKey) {
        final int pos = currentKey.indexOf('_');
        final String crawlID = currentKey.substring(0, pos).replaceAll("%5F", "_");
        int pos2 = currentKey.indexOf('_', pos + 1);
        // no separator? just normalise whatever is left
        if (pos2 == -1) pos2 = currentKey.length();
        final String queueID = currentKey.substring(pos + 1, pos2).replaceAll("%5F", "_");
        return QueueWithinCrawl.get(queueID, crawlID);
    }
}
