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

package crawlercommons.urlfrontier.service.memory;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import io.grpc.stub.StreamObserver;

/**
 * A simple implementation of a URL Frontier service using in memory data
 * structures. Useful for testing the API.
 **/

public class MemoryFrontierService extends AbstractFrontierService {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MemoryFrontierService.class);

	protected Map<String, URLQueue> queues;

	public MemoryFrontierService(){
		queues = Collections.synchronizedMap(new LinkedHashMap<String, URLQueue>());
	}
	
	@Override
	public void getURLs(GetParams request, StreamObserver<URLInfo> responseObserver) {
		// on hold
		if (!isActive()) {
			responseObserver.onCompleted();
			return;
		}

		int maxQueues = request.getMaxQueues();
		int maxURLsPerQueue = request.getMaxUrlsPerQueue();
		int secsUntilRequestable = request.getDelayRequestable();

		// 0 by default
		if (maxQueues == 0) {
			maxQueues = Integer.MAX_VALUE;
		}

		if (maxURLsPerQueue == 0) {
			maxURLsPerQueue = Integer.MAX_VALUE;
		}

		if (secsUntilRequestable == 0) {
			secsUntilRequestable = 30;
		}

		LOG.info("Received request to get fetchable URLs [max queues {}, max URLs {}, delay {}]", maxQueues,
				maxURLsPerQueue, secsUntilRequestable);

		long start = System.currentTimeMillis();

		String key = request.getKey();

		long now = Instant.now().getEpochSecond();

		// want a specific key only?
		// default is an empty string so should never be null
		if (key != null && key.length() >= 1) {
			URLQueue queue = queues.get(key);

			// the queue does not exist
			if (queue == null) {
				responseObserver.onCompleted();
				return;
			}

			// it is locked
			if (queue.getBlockedUntil() >= now) {
				responseObserver.onCompleted();
				return;
			}

			// too early?
			int delay = queue.getDelay();
			if (delay == -1)
				delay = getDefaultDelayForQueues();
			if (queue.getLastProduced() + delay >= now) {
				responseObserver.onCompleted();
				return;
			}

			int totalSent = sendURLsForQueue(queue, key, maxURLsPerQueue, secsUntilRequestable, now, responseObserver);
			responseObserver.onCompleted();

			LOG.info("Sent {} from queue {} in {} msec", totalSent, key, (System.currentTimeMillis() - start));

			if (totalSent != 0) {
				queue.setLastProduced(now);
			}

			return;
		}

		int numQueuesSent = 0;
		int totalSent = 0;
		// to make sure we don't loop over the ones we already had
		String firstQueue = null;

		synchronized (queues) {
			while (!queues.isEmpty() && numQueuesSent < maxQueues) {
				Iterator<Entry<String, URLQueue>> iterator = queues.entrySet().iterator();
				Entry<String, URLQueue> e = iterator.next();
				if (firstQueue == null) {
					firstQueue = e.getKey();
				} else if (firstQueue.equals(e.getKey())) {
					break;
				}
				// We remove the entry and put it at the end of the map
				iterator.remove();

				// Put the entry at the end
				queues.put(e.getKey(), e.getValue());

				// it is locked
				if (e.getValue().getBlockedUntil() >= now) {
					continue;
				}

				// too early?
				int delay = e.getValue().getDelay();
				if (delay == -1)
					delay = getDefaultDelayForQueues();
				if (e.getValue().getLastProduced() + delay >= now) {
					continue;
				}

				int sentForQ = sendURLsForQueue(e.getValue(), e.getKey(), maxURLsPerQueue, secsUntilRequestable, now,
						responseObserver);
				if (sentForQ > 0) {
					e.getValue().setLastProduced(now);
					totalSent += sentForQ;
					numQueuesSent++;
				}

			}
		}

		LOG.info("Sent {} from {} queue(s) in {} msec", totalSent, numQueuesSent, (System.currentTimeMillis() - start));

		responseObserver.onCompleted();
	}

	/**
	 * @return true if at least one URL has been sent for this queue, false
	 *         otherwise
	 **/
	private int sendURLsForQueue(final PriorityQueue<InternalURL> queue, final String key, final int maxURLsPerQueue,
			final int secsUntilRequestable, final long now, final StreamObserver<URLInfo> responseObserver) {
		Iterator<InternalURL> iter = queue.iterator();
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
				item.heldUntil = Instant.now().plusSeconds(secsUntilRequestable).getEpochSecond();

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

				// if not set use the hostname
				if (key.equals("")) {
					try {
						URL u = new URL(iu.url);
						key = u.getHost();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				// get the priority queue or create one
				synchronized (queues) {
					URLQueue queue = queues.get(key);
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

	@Override
	public void getStats(crawlercommons.urlfrontier.Urlfrontier.String request,
			StreamObserver<Stats> responseObserver) {
		LOG.info("Received stats request");

		final Map<String, Long> s = new HashMap<>();

		int inProc = 0;
		int numQueues = 0;
		int size = 0;
		long completed = 0;

		Collection<URLQueue> _queues = queues.values();

		// specific queue?
		if (!request.getValue().isEmpty()) {
			URLQueue q = queues.get(request.getValue());
			if (q != null) {
				_queues = new LinkedList<>();
				_queues.add(q);
			} else {
				// TODO notify an error to the client
			}
		}

		long now = Instant.now().getEpochSecond();

		for (URLQueue q : _queues) {
			inProc += q.getInProcess(now);
			numQueues++;
			size += q.size();
			completed += q.getCountCompleted();
		}

		// put count completed as custom stats for now
		// add it as a proper field later?
		s.put("completed", completed);

		Stats stats = Stats.newBuilder().setNumberOfQueues(numQueues).setSize(size).setInProcess(inProc).putAllCounts(s)
				.build();
		responseObserver.onNext(stats);
		responseObserver.onCompleted();
	}

	@Override
	public void listQueues(crawlercommons.urlfrontier.Urlfrontier.Integer request,
			StreamObserver<StringList> responseObserver) {
		long maxQueues = request.getValue();
		// 0 by default
		if (maxQueues == 0) {
			maxQueues = Long.MAX_VALUE;
		}

		LOG.info("Received request to list queues [max {}]", maxQueues);

		long now = Instant.now().getEpochSecond();
		int num = 0;
		Builder list = StringList.newBuilder();

		Iterator<Entry<String, URLQueue>> iterator = queues.entrySet().iterator();
		while (iterator.hasNext() && num <= maxQueues) {
			Entry<String, URLQueue> e = iterator.next();
			// check that it isn't blocked
			if (e.getValue().getBlockedUntil() >= now) {
				continue;
			}

			// check that the queue has URLs due for fetching
			if (e.getValue().peek().nextFetchDate <= now) {
				list.addValues(e.getKey());
				num++;
			}
		}
		responseObserver.onNext(list.build());
		responseObserver.onCompleted();
	}

	/**
	 * <pre>
	 ** Delete  the queue based on the key in parameter *
	 * </pre>
	 */
	@Override
	public void deleteQueue(crawlercommons.urlfrontier.Urlfrontier.String request,
			StreamObserver<Empty> responseObserver) {
		queues.remove(request.getValue());
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void blockQueueUntil(BlockQueueParams request, StreamObserver<Empty> responseObserver) {
		URLQueue queue = queues.get(request.getKey());
		if (queue != null) {
			queue.setBlockedUntil(request.getTime());
		}
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void setDelay(QueueDelayParams request, StreamObserver<Empty> responseObserver) {
		String key = request.getKey();
		if (key.isEmpty()) {
			setDefaultDelayForQueues(request.getDelayRequestable());
		} else {
			URLQueue queue = queues.get(request.getKey());
			if (queue != null) {
				queue.setDelay(request.getDelayRequestable());
			}
		}
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

}
