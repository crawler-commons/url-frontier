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
}
