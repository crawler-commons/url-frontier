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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
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

	// sorted the queues by next fetch date of the first element they contain
	private SortedMap<String, URLQueue> queues = new ConcurrentSkipListMap<String, URLQueue>();

	/** Queues are ordered by the nextfetchdate of their first element **/
	static class URLQueue extends PriorityQueue<InternalURL> implements Comparable<URLQueue> {

		URLQueue(InternalURL initial) {
			this.add(initial);
		}

		@Override
		public int compareTo(URLQueue target) {
			InternalURL mine = this.peek();
			InternalURL his = target.peek();
			return mine.compareTo(his);
		}

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
			if (i.getDiscovered() != null) {
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
			// check that the queue has URLs due for fetching
			if (e.getValue().peek().nextFetchDate < now) {
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
			PriorityQueue<InternalURL> queue = queues.get(key);

			// the queue does not exist
			if (queue == null) {
				responseObserver.onCompleted();
				return;
			}

			int totalSent = sendURLsForQueue(queue, key, maxURLsPerQueue, secsUntilRequestable, now, responseObserver);
			responseObserver.onCompleted();

			LOG.info("Sent {} from queue {} in {}", totalSent, key, (System.currentTimeMillis() - start));

			return;
		}

		Iterator<Entry<String, URLQueue>> iterator = queues.entrySet().iterator();
		int numQueuesSent = 0;
		int totalSent = 0;
		while (iterator.hasNext() && numQueuesSent < maxQueues) {
			Entry<String, URLQueue> e = iterator.next();
			int sentForQ = sendURLsForQueue(e.getValue(), e.getKey(), maxURLsPerQueue, secsUntilRequestable, now,
					responseObserver);
			if (sentForQ > 0) {
				totalSent += sentForQ;
				numQueuesSent++;
			}
		}

		LOG.info("Sent {} from {} queue(s) in {}", totalSent, numQueuesSent, (System.currentTimeMillis() - start));

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
						responseObserver.onNext(
								crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(iu.url).build());
						return;
					} else {
						// overwrite the existing version
						queue.remove(iu);
					}
				}

				// add the new item
				queue.add(iu);
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
				// create a new queue so that the entries get sorted
				queues = new ConcurrentSkipListMap<String, URLQueue>(queues);
				responseObserver.onCompleted();
			}
		};

	}

	@Override
	public void getStats(crawlercommons.urlfrontier.Urlfrontier.String request,
			io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {

		LOG.info("Received stats request");

		final Map<String, Integer> s = new HashMap<>();

		int inProc = 0;
		int numQueues = 0;
		int size = 0;

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
		}

		Stats stats = Stats.newBuilder().setNumberOfQueues(numQueues).setSize(size).setInProcess(inProc).putAllCounts(s)
				.build();
		responseObserver.onNext(stats);
		responseObserver.onCompleted();
		return;

	}

}
