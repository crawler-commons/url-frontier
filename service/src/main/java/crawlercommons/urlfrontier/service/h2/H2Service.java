/**
 * Licensed to Crawler-Commons under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * DigitalPebble licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package crawlercommons.urlfrontier.service.h2;

import java.io.Closeable;
import java.io.IOException;

import org.h2.mvstore.MVStore;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;

public class H2Service extends MemoryFrontierService implements Closeable {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(H2Service.class);

	private MVStore store;

	public H2Service(JsonNode configurationNode) {

		// connect to ES
		String path = "./h2";
		JsonNode tempNode = configurationNode.get("h2.path");
		if (tempNode != null && !tempNode.isNull()) {
			path = tempNode.asText(path);
		}

		store = MVStore.open(path);

		queues = store.openMap("QUEUES");

		LOG.info("{} queues loaded", queues.size());
	}

	@Override
	public void close() throws IOException {
		if (store != null) {
			try {
				store.close();
			} catch (Exception e) {
				LOG.error("Closing ", e);
			}
		}

	}

}
