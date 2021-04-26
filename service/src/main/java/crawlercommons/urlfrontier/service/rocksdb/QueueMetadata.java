/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons
 * SPDX-License-Identifier: Apache-2.0
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

package crawlercommons.urlfrontier.service.rocksdb;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import crawlercommons.urlfrontier.service.QueueInterface;

public class QueueMetadata implements QueueInterface {

	QueueMetadata() {
	}

	/** number of URLs that are not scheduled anymore **/
	private AtomicInteger completed = new AtomicInteger(0);

	/** number of URLs scheduled in the queue **/
	private AtomicInteger active = new AtomicInteger(0);

	private long blockedUntil = -1;

	private int delay = -1;

	private long lastProduced = 0;

	private Map<String, Long> beingProcessed = null;

	@Override
	public int getInProcess(long now) {
		synchronized (this) {
			if (beingProcessed == null)
				return 0;
			// check that the content of beingProcessed is still valid
			beingProcessed.entrySet().removeIf(e -> {
				return e.getValue().longValue() <= now;
			});
			return beingProcessed.size();
		}
	}

	public void holdUntil(String url, long timeinSec) {
		synchronized (this) {
			if (beingProcessed == null)
				beingProcessed = new LinkedHashMap<>();
			beingProcessed.put(url, timeinSec);
		}
	}

	public boolean isHeld(String url, long now) {
		synchronized (this) {
			if (beingProcessed == null)
				return false;
			Long timeout = beingProcessed.get(url);
			if (timeout != null) {
				if (timeout.longValue() < now) {
					// release!
					beingProcessed.remove(url);
					return false;
				} else
					return true;
			}
			return false;
		}
	}

	public void removeFromProcessed(String url) {
		synchronized (this) {
			// should not happen
			if (beingProcessed == null)
				return;

			// remove from ephemeral cache of URLs in process
			beingProcessed.remove(url);
		}
	}

	@Override
	public int getCountCompleted() {
		return completed.get();
	}

	@Override
	public void setBlockedUntil(long until) {
		blockedUntil = until;
	}

	@Override
	public long getBlockedUntil() {
		return blockedUntil;
	}

	@Override
	public int getDelay() {
		return delay;
	}

	@Override
	public void setDelay(int delayRequestable) {
		this.delay = delayRequestable;
	}

	@Override
	public long getLastProduced() {
		return lastProduced;
	}

	@Override
	public void setLastProduced(long lastProduced) {
		this.lastProduced = lastProduced;
	}

	public void decrementActive() {
		active.decrementAndGet();
	}
	
	public void incrementActive() {
		active.incrementAndGet();
	}

	public void incrementCompleted() {
		completed.incrementAndGet();
	}

	@Override
	public int countActive() {
		return active.get();
	}

}