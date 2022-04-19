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
            LOG.info("Sending heartbeat");

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

            sendHeartBeat();
        }
    }

    protected void sendHeartBeat() {}
}
