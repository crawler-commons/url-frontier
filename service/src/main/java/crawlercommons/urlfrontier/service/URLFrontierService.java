package crawlercommons.urlfrontier.service;

import java.util.Iterator;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.stub.StreamObserver;

public class URLFrontierService extends crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierImplBase {

	private final java.util.Map<String, URLItem> queues;

	public URLFrontierService() {
		queues = new java.util.HashMap<>();
	}

	@Override
	public void listQueues(GetParams request, StreamObserver<StringList> responseObserver) {
		int maxQueues = request.getMaxQueues();
		// 0 by default but just in case a negative value is set
		if (maxQueues < 1) {
			maxQueues = Integer.MAX_VALUE;
		}
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
		// TODO Auto-generated method stub
		super.getURLs(request, responseObserver);
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
