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

package crawlercommons.urlfrontier.service.memory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;

import crawlercommons.urlfrontier.service.QueueInterface;

public class URLQueue extends PriorityQueue<InternalURL> implements QueueInterface {

	public URLQueue(InternalURL initial) {
		this.add(initial);
	}

	// keep a hash of the completed URLs
	// these won't be refetched

	private HashSet<String> completed = new HashSet<>();

	private long blockedUntil = -1;

	private int delay = -1;

	private long lastProduced = 0;

	@Override
	public int getInProcess(long now) {
		// a URL in process has a heldUntil and is at the beginning of a queue
		Iterator<InternalURL> iter = this.iterator();
		int inproc = 0;
		while (iter.hasNext()) {
			InternalURL iu = iter.next();
			if (iu.heldUntil > now)
				inproc++;
			// can stop if no heldUntil at all
			else if (iu.heldUntil == -1)
				return inproc;
		}
		return inproc;
	}

	@Override
	public boolean contains(Object iu) {
		// been fetched before?
		if (completed.contains(((InternalURL) iu).url)) {
			return true;
		}
		return super.contains(iu);
	}

	public void addToCompleted(String url) {
		completed.add(url);
	}

	@Override
	public int getCountCompleted() {
		return completed.size();
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

	@Override
	public int getDelay() {
		return delay;
	}

	@Override
	public int countActive() {
		return this.size();
	}

}