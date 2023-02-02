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
package crawlercommons.urlfrontier.service.ignite;

import crawlercommons.urlfrontier.service.cluster.Hearbeat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.cache.Cache.Entry;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;

/** The frontiers cache must have been created beforehand * */
public class IgniteHeartbeat extends Hearbeat {

    private final Ignite ignite;

    IgniteHeartbeat(int delay, Ignite ignite) {
        super("IgniteHeartbeat", delay);
        this.ignite = ignite;
    }

    @Override
    protected void sendHeartBeat() {
        IgniteCache<String, String> frontiers = ignite.cache(IgniteService.frontiersCacheName);
        frontiers.put(listener.getAddress(), Instant.now().toString());

        List<String> activeFrontiers = new ArrayList<>();

        // get all the active frontiers and notify the listener about them
        try (QueryCursor<Entry<String, String>> cur =
                frontiers.query(new ScanQuery<String, String>())) {
            for (Entry<String, String> entry : cur) {
                activeFrontiers.add(entry.getKey());
            }
        }

        listener.setNodes(activeFrontiers);
    }
}
