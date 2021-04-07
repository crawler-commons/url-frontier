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

package crawlercommons.urlfrontier.service.rocksdb;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueueMetadata {

	QueueMetadata() {
	}

	private long completed = 0;

	private long blockedUntil = -1;

	private int delay = -1;

	private long lastProduced = 0;

	private Map<String, Long> beingProcessed = null;

	public int getInProcess() {
		if (beingProcessed == null)
			return 0;
		return beingProcessed.size();
	}

	public void holdUntil(String url, long now) {
		if (beingProcessed == null)
			beingProcessed = new LinkedHashMap<>();
		beingProcessed.put(url, now);
	}

	public boolean isHeld(String url, long now) {
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
		} else {
			// should not happen
			beingProcessed.remove(url);
		}
		return false;
	}

	public void addToCompleted(String url) {
		// should not happen
		if (beingProcessed == null)
			return;

		// remove from ephemeral cache of URLs in process?
		Long timeout = beingProcessed.remove(url);

		if (timeout != null)
			completed++;
	}

	public long getCountCompleted() {
		return completed;
	}

	public void setBlockedUntil(long until) {
		blockedUntil = until;
	}

	public long getBlockedUntil() {
		return blockedUntil;
	}

	public void setDelay(int delayRequestable) {
		this.delay = delayRequestable;
	}

	public long getLastProduced() {
		return lastProduced;
	}

	public void setLastProduced(long lastProduced) {
		this.lastProduced = lastProduced;
	}

	public int getDelay() {
		return delay;
	}

}