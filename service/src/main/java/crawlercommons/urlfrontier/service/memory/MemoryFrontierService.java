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

import java.time.Instant;
import java.util.Iterator;
import java.util.PriorityQueue;

import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueInterface;
import io.grpc.stub.StreamObserver;

/**
 * A simple implementation of a URL Frontier service using in memory data
 * structures. Useful for testing the API.
 **/

public class MemoryFrontierService extends AbstractFrontierService {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MemoryFrontierService.class);

	public MemoryFrontierService() {
	}

	/**
	 * @return true if at least one URL has been sent for this queue, false
	 *         otherwise
	 **/

	@Override
	protected int sendURLsForQueue(QueueInterface queue, String key, int maxURLsPerQueue, int secsUntilRequestable,
			long now, StreamObserver<URLInfo> responseObserver) {
		Iterator<InternalURL> iter = ((PriorityQueue<InternalURL>) queue).iterator();
		int alreadySent = 0;

		while (iter.hasNext() && alreadySent < maxURLsPerQueue) {
			InternalURL item = iter.next();

			// check that is is due
			if (item.nextFetchDate > now) {
				// they are sorted by date no need to go further
				return alreadySent;
			}

			// check that the URL is not already being processed
			if (item.heldUntil > now) {
				continue;
			}

			// this one is good to go
			try {
				responseObserver.onNext(item.toURLInfo(key));

				// mark it as not processable for N secs
				item.heldUntil = now + secsUntilRequestable;

				alreadySent++;
			} catch (InvalidProtocolBufferException e) {
				LOG.error("Caught unlikely error ", e);
			}
		}

		return alreadySent;
	}

	@Override
	public StreamObserver<URLItem> putURLs(
			StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {

		return new StreamObserver<URLItem>() {

			@Override
			public void onNext(URLItem value) {

				Object[] parsed = InternalURL.from(value);

				String key = (String) parsed[0];
				Boolean discovered = (Boolean) parsed[1];
				InternalURL iu = (InternalURL) parsed[2];

				// has a queue key been defined? if not use the hostname
				if (key.equals("")) {
					LOG.debug("key missing for {}", iu.url);
					key = provideMissingKey(iu.url);
					if (key == null) {
						LOG.error("Malformed URL {}", iu.url);
						responseObserver.onNext(
								crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(iu.url).build());
						return;
					}
				}

				// get the priority queue or create one
				synchronized (queues) {
					URLQueue queue = (URLQueue) queues.get(key);
					if (queue == null) {
						queues.put(key, new URLQueue(iu));
						// ack reception of the URL
						responseObserver.onNext(
								crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(iu.url).build());
						return;
					}

					// check whether the URL already exists
					if (queue.contains(iu)) {
						if (discovered) {
							// we already discovered it - so no need for it
							responseObserver.onNext(crawlercommons.urlfrontier.Urlfrontier.String.newBuilder()
									.setValue(iu.url).build());
							return;
						} else {
							// overwrite the existing version
							queue.remove(iu);
						}
					}

					// add the new item
					// unless it is an update and it's nextFetchDate is 0 == NEVER
					if (!discovered && iu.nextFetchDate == 0) {
						queue.addToCompleted(iu.url);
					} else {
						queue.add(iu);
					}
				}

				responseObserver
						.onNext(crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(iu.url).build());
			}

			@Override
			public void onError(Throwable t) {
				LOG.error("Throwable caught", t);
			}

			@Override
			public void onCompleted() {
				// will this ever get called if the client is constantly streaming?
				responseObserver.onCompleted();
			}
		};
	}
}
