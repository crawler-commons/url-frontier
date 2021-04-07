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

public class QueueMetadata {

	QueueMetadata() {
	}

	private long completed = 0;

	private long blockedUntil = -1;

	private int delay = -1;

	private long lastProduced = 0;

	private int inproc = 0;

	/** used to rank the URLs in the FIFO queue **/
	private long lastPrefix = -1;

	public long incrememntAndGetLastPrefix() {
		return ++lastPrefix;
	}

	public int getInProcess(long now) {
		return inproc;
	}

	public void addToCompleted(String url) {
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