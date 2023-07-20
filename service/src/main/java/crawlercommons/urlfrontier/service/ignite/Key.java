// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.ignite;

import java.io.Serializable;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;

class Key implements Serializable {

    @AffinityKeyMapped String crawlQueueID;

    String URL;

    Key(String crawlQueueID, String uRL) {
        super();
        this.crawlQueueID = crawlQueueID;
        URL = uRL;
    }

    public String toString() {
        return this.crawlQueueID + "_" + this.URL;
    }
}
