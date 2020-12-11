package crawlercommons.urlfrontier.service;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLItem.Status;
import io.grpc.stub.StreamObserver;

/**
 * Dummy implementation of a URL Frontier service using in memory data
 * structures. Useful for testing the API.
 **/

public class DummyURLFrontierService extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DummyURLFrontierService.class);

	private final java.util.Map<String, PriorityQueue<URLItem>> queues;

	/**
	 * simpler than the objects from gRPC + sortable and have equals based on URL
	 * only
	 **/
	static class InternalURL implements Comparable<InternalURL> {

		Instant nextFetchDate;
		int status;
		String url;
		Map<String, StringList> metadata;

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

		@Override
		public int hashCode() {
			return url.hashCode();
		}

	}

	public DummyURLFrontierService() {
		queues = new java.util.HashMap<>();
	}

	@Override
	public void listQueues(GetParams request, StreamObserver<StringList> responseObserver) {

		int maxQueues = request.getMaxQueues();
		// 0 by default but just in case a negative value is set
		if (maxQueues < 1) {
			maxQueues = Integer.MAX_VALUE;
		}

		LOG.info("Received request to list queues [max {}]", maxQueues);

		Iterator<String> iterator = queues.keySet().iterator();
		int num = 0;
		Builder list = StringList.newBuilder();
		while (iterator.hasNext() && num <= maxQueues) {
			list.addString(iterator.next());
		}
		responseObserver.onNext(list.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getURLs(GetParams request, StreamObserver<URLItem> responseObserver) {
		throw new RuntimeException("getURLs() not implemented");
	}

	@Override
	public StreamObserver<URLItem> putURLs(StreamObserver<Empty> responseObserver) {

		return new StreamObserver<URLItem>() {

			@Override
			public void onNext(URLItem value) {
				// get the priority queue or create one
				PriorityQueue queue = queues.computeIfAbsent(value.getKey(), (k) -> new PriorityQueue());

				InternalURL iu = InternalURL.from(value);

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
				// TODO Auto-generated method stub

			}

			@Override
			public void onCompleted() {
				responseObserver.onNext(Empty.newBuilder().build());
				responseObserver.onCompleted();
			}
		};

	}

}
