package crawlercommons.urlfrontier.service;

import java.util.Iterator;

import org.slf4j.LoggerFactory;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.stub.StreamObserver;

/**
 * Dummy implementation of a URL Frontier service using in memory data
 * structures. Useful for testing the API.
 **/

public class DummyURLFrontierService extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DummyURLFrontierService.class);

	private final java.util.Map<String, URLItem> queues;

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
				queues.put(value.getKey(), value);
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
