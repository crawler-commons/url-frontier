/**
 * SPDX-FileCopyrightText: 2022 Crawler-commons SPDX-License-Identifier: Apache-2.0 Licensed to
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
package crawlercommons.urlfrontier.service.cluster;

import crawlercommons.urlfrontier.service.AbstractFrontierService;
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

    private AbstractFrontierService service;

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

    public void setService(AbstractFrontierService service) {
        this.service = service;
    }

    @Override
    public void run() {

        // wait until the service is set up to prevent a race condition
        if (service != null) {
            while (!service.isReady()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            ;
        }

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
