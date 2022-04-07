package crawlercommons.urlfrontier.service.ignite;

import java.io.Serializable;

class SchedulingKey extends Key implements Serializable {

    static final SchedulingKey dummy = new SchedulingKey(null, null, 0);

    long nextFetchDate;

    SchedulingKey(String crawlQueueID, String uRL, long nextFetchDate) {
        super(crawlQueueID, uRL);
        this.nextFetchDate = nextFetchDate;
    }
}
