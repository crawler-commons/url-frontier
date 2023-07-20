// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import java.time.Instant;
import org.slf4j.LoggerFactory;

/**
 * Started by Frontiers instances to send a heartbeat to a backend and as a result to be able to
 * list all the Frontier instances being part of the cluster.
 */
public class Hearbeat extends Thread {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(Hearbeat.class);

    private final int delaySec;

    private boolean closed = false;

    Instant lastQuery = Instant.EPOCH;

    protected HeartbeatListener listener;

    protected Hearbeat(int delay) {
        super("Hearbeat");
        this.delaySec = delay;
    }

    protected Hearbeat(String name, int delay) {
        super(name);
        this.delaySec = delay;
    }

    public void close() {
        closed = true;
    }

    public void setListener(HeartbeatListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {

        while (!closed) {

            // implement delay between requests
            long msecTowait =
                    delaySec * 1000 - (Instant.now().toEpochMilli() - lastQuery.toEpochMilli());
            if (msecTowait > 0) {
                try {
                    Thread.sleep(msecTowait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            lastQuery = Instant.now();

            LOG.debug("Sending heartbeat");

            sendHeartBeat();
        }
    }

    protected void sendHeartBeat() {}
}
