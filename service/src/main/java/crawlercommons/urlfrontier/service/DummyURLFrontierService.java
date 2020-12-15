package crawlercommons.urlfrontier.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.Timestamp;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLItem.Status;
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
				if (iter.next().heldUntil != null)
					inproc++;
				else
					return inproc;
			}
			return inproc;
		}

		public Map<String, Integer> getStats() {
			// a URL in process has a heldUntil and is at the beginning of a queue
			Iterator<InternalURL> iter = this.iterator();
			int[] statusCount = new int[5];
			while (iter.hasNext()) {
				statusCount[iter.next().status]++;
			}
			// convert from index to string value
			Map<String, Integer> stats = new HashMap<>();

			for (int rank = 0; rank < statusCount.length; rank++) {
				stats.put(Status.forNumber(rank).name(), statusCount[rank]);
			}
			return stats;
		}
	}

	/**
	 * simpler than the objects from gRPC + sortable and have equals based on URL
	 * only
	 **/
	static class InternalURL implements Comparable<InternalURL> {

		Instant nextFetchDate;
		int status;
		String url;
		Map<String, StringList> metadata;

		// this is set when the URL is sent for processing
		// so that a subsequent call to getURLs does not send it again
		Instant heldUntil;

		private InternalURL() {
		}

		public static InternalURL from(URLItem i) {
			InternalURL iu = new InternalURL();
			iu.url = i.getUrl();
			iu.status = i.getStatus().getNumber();
			iu.nextFetchDate = Instant.ofEpochSecond(i.getNextFetchDate().getSeconds(),
					i.getNextFetchDate().getNanos());
			iu.metadata = i.getMetadataMap();
			return iu;
		}

		@Override
		public int compareTo(InternalURL arg0) {
			int comp = nextFetchDate.compareTo(arg0.nextFetchDate);
			if (comp == 0) {
				return url.compareTo(arg0.url);
			}
			return comp;
		}

		@Override
		public boolean equals(Object obj) {
			return url.equals(((InternalURL) obj).url);
		}

		void setHeldUntil(Instant t) {
			heldUntil = t;
		}

		@Override
		public int hashCode() {
			return url.hashCode();
		}

		public URLItem toURLItem(String key) {
			Timestamp ts = Timestamp.newBuilder().setSeconds(nextFetchDate.getEpochSecond())
					.setNanos(nextFetchDate.getNano()).build();
			crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder = URLItem.newBuilder().setNextFetchDate(ts)
					.setStatus(Status.forNumber(status)).setKey(key).setUrl(url);
			builder.putAllMetadata(metadata);
			return builder.build();
		}

	}

	@Override
	public void listQueues(GetParams request, StreamObserver<StringList> responseObserver) {

		int maxQueues = request.getMaxQueues();
		// 0 by default
		if (maxQueues == 0) {
			maxQueues = Integer.MAX_VALUE;
		}

		LOG.info("Received request to list queues [max {}]", maxQueues);

		Instant now = Instant.now();
		int num = 0;
		Builder list = StringList.newBuilder();

		Iterator<Entry<String, URLQueue>> iterator = queues.entrySet().iterator();
		while (iterator.hasNext() && num <= maxQueues) {
			Entry<String, URLQueue> e = iterator.next();
			// check that the queue has URLs due for fetching
			if (e.getValue().peek().nextFetchDate.isBefore(now)) {
				list.addString(e.getKey());
				num++;
			}
		}
		responseObserver.onNext(list.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getURLs(GetParams request, StreamObserver<URLItem> responseObserver) {
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

		String key = request.getKey();

		Instant now = Instant.now();

		// want a specific key only?
		// default is an empty string so should never be null
		if (key != null && key.length() >= 1) {
			PriorityQueue<InternalURL> queue = queues.get(key);

			// the queue does not exist
			if (queue == null) {
				responseObserver.onCompleted();
				return;
			}

			sendURLsForQueue(queue, key, maxURLsPerQueue, secsUntilRequestable, now, responseObserver);
			responseObserver.onCompleted();
			return;
		}

		Iterator<Entry<String, URLQueue>> iterator = queues.entrySet().iterator();
		int num = 0;
		while (iterator.hasNext() && num <= maxQueues) {
			Entry<String, URLQueue> e = iterator.next();
			boolean sentOne = sendURLsForQueue(e.getValue(), e.getKey(), maxURLsPerQueue, secsUntilRequestable, now,
					responseObserver);
			if (sentOne) {
				num++;
			}
		}
		responseObserver.onCompleted();
	}

	/**
	 * @return true if at least one URL has been sent for this queue, false
	 *         otherwise
	 **/
	private boolean sendURLsForQueue(final PriorityQueue<InternalURL> queue, final String key,
			final int maxURLsPerQueue, final int secsUntilRequestable, final Instant now,
			final StreamObserver<URLItem> responseObserver) {
		Iterator<InternalURL> iter = queue.iterator();
		int alreadySent = 0;
		boolean oneFoundForThisQ = false;

		while (iter.hasNext() && alreadySent < maxURLsPerQueue) {
			InternalURL item = iter.next();

			// check that is is due
			if (item.nextFetchDate.isAfter(now)) {
				return oneFoundForThisQ;
			}

			// check that the URL is not already being processed
			if (item.heldUntil != null && item.heldUntil.isAfter(now)) {
				continue;
			}

			// this one is good to go

			// count one queue
			oneFoundForThisQ = true;

			URLItem converted = item.toURLItem(key);
			responseObserver.onNext(converted);

			// mark it as not processable for N secs
			item.heldUntil = Instant.now().plusSeconds(secsUntilRequestable);

			alreadySent++;
		}

		return oneFoundForThisQ;
	}

	@Override
	public StreamObserver<URLItem> putURLs(StreamObserver<Empty> responseObserver) {

		return new StreamObserver<URLItem>() {

			@Override
			public void onNext(URLItem value) {
				// get the priority queue or create one
				InternalURL iu = InternalURL.from(value);

				URLQueue queue = queues.get(value.getKey());
				if (queue == null) {
					queues.put(value.getKey(), new URLQueue(iu));
					return;
				}

				// check whether the URL already exists
				if (queue.contains(iu)) {
					if (value.getStatus().getNumber() == Status.DISCOVERED_VALUE) {
						// we already discovered it - so no need for it
						return;
					} else {
						// overwrite the existing version
						queue.remove(iu);
					}
				}

				// add the new item
				queue.add(iu);
			}

			@Override
			public void onError(Throwable t) {
				LOG.error("Throwable caught", t);
			}

			@Override
			public void onCompleted() {
				// create a new queue so that the entries get sorted
				queues = new ConcurrentSkipListMap<String, URLQueue>(queues);
				responseObserver.onNext(Empty.newBuilder().build());
				responseObserver.onCompleted();
			}
		};

	}

	public void stats(crawlercommons.urlfrontier.Urlfrontier.String request,
			io.grpc.stub.StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Stats> responseObserver) {
		// empty request - want for the whole crawl
		if (request.getValue().isEmpty()) {
			// TODO
			super.stats(request, responseObserver);
			return;
		}

		// get the stats for a specific queue
		URLQueue q = queues.get(request.getValue());
		if (q != null) {
			Stats stats = Stats.newBuilder().setNumberOfQueues(1).setSize(q.size()).setInProcess(q.getInProcess())
					.putAllCountsPerStatus(q.getStats()).build();
			responseObserver.onNext(stats);
		}
		responseObserver.onCompleted();

	}

}
