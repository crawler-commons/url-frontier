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

package crawlercommons.urlfrontier.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.stub.StreamObserver;

/**
 * Dummy implementation of a URL Frontier service using in memory data
 * structures. Useful for testing the API.
 **/

public class DummyURLFrontierService extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DummyURLFrontierService.class);

	private Map<String, URLQueue> queues = Collections.synchronizedMap(new LinkedHashMap<String, URLQueue>());

	private boolean active = true;

	private int defaultDelayForQueues = 1;

	static class URLQueue extends PriorityQueue<InternalURL> {

		URLQueue(InternalURL initial) {
			this.add(initial);
		}

		// keep a hash of the completed URLs
		// these won't be refetched

		private HashSet<String> completed = new HashSet<>();

		private long blockedUntil = -1;

		private int delay = -1;

		private long lastProduced = 0;

		public int getInProcess() {
			// a URL in process has a heldUntil and is at the beginning of a queue
			Iterator<InternalURL> iter = this.iterator();
			int inproc = 0;
			while (iter.hasNext()) {
				if (iter.next().heldUntil != -1)
					inproc++;
				else
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

		public int getCountCompleted() {
			return completed.size();
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

	/**
	 * simpler than the objects from gRPC + sortable and have equals based on URL
	 * only
	 **/
	static class InternalURL implements Comparable<InternalURL> {

		long nextFetchDate;
		String url;
		Map<String, StringList> metadata;

		// this is set when the URL is sent for processing
		// so that a subsequent call to getURLs does not send it again
		long heldUntil = -1;

		private InternalURL() {
		}

		/*
		 * Returns the key if any, whether it is a discovered URL or not and an internal
		 * object to represent it
		 **/
		private static Object[] from(URLItem i) {
			InternalURL iu = new InternalURL();
			URLInfo info;
			Boolean disco = Boolean.TRUE;
			if (i.hasDiscovered()) {
				info = i.getDiscovered().getInfo();
				iu.nextFetchDate = Instant.now().getEpochSecond();
			} else {
				KnownURLItem known = i.getKnown();
				info = known.getInfo();
				iu.nextFetchDate = known.getRefetchableFromDate();
				disco = Boolean.FALSE;
			}
			iu.metadata = info.getMetadataMap();
			iu.url = info.getUrl();
			return new Object[] { info.getKey(), disco, iu };
		}

		@Override
		public int compareTo(InternalURL arg0) {
			int comp = Long.compare(nextFetchDate, arg0.nextFetchDate);
			if (comp == 0) {
				return url.compareTo(arg0.url);
			}
			return comp;
		}

		@Override
		public boolean equals(Object obj) {
			return url.equals(((InternalURL) obj).url);
		}

		void setHeldUntil(long t) {
			heldUntil = t;
		}

		@Override
		public int hashCode() {
			return url.hashCode();
		}

		public URLInfo toURLInfo(String key) {
			return URLInfo.newBuilder().setKey(key).setUrl(url).putAllMetadata(metadata).build();
		}

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
			io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Empty> responseObserver) {
		queues.remove(request.getValue());
	}

	@Override
	public void getURLs(GetParams request, StreamObserver<URLInfo> responseObserver) {

		// on hold
		if (!active) {
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
				delay = defaultDelayForQueues;
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
					delay = defaultDelayForQueues;
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
			if (item.heldUntil != -1 && item.heldUntil > now) {
				continue;
			}

			// this one is good to go
			responseObserver.onNext(item.toURLInfo(key));

			// mark it as not processable for N secs
			item.heldUntil = Instant.now().plusSeconds(secsUntilRequestable).getEpochSecond();

			alreadySent++;
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
			io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {

		LOG.info("Received stats request");

		final Map<String, Long> s = new HashMap<>();

		int inProc = 0;
		int numQueues = 0;
		int size = 0;
		long completed = 0;

		Collection<URLQueue> _queues = queues.values();

		// specific queue?
		if (request.getValue().isEmpty()) {
			URLQueue q = queues.get(request.getValue());
			if (q != null) {
				_queues = new LinkedList<>();
				_queues.add(q);
			} else {
				// TODO notify an error to the client
			}
		}

		for (URLQueue q : _queues) {
			inProc += q.getInProcess();
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
	public void setActive(crawlercommons.urlfrontier.Urlfrontier.Boolean request,
			StreamObserver<Empty> responseObserver) {
		active = request.getState();
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
			this.defaultDelayForQueues = request.getDelayRequestable();
		} else {
			URLQueue queue = queues.get(request.getKey());
			if (queue != null) {
				queue.setDelay(request.getDelayRequestable());
			}
		}
	}

}
