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

package crawlercommons.urlfrontier.service.elasticsearch;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.Integer;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.StringList.Builder;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.InternalURL;
import io.grpc.stub.StreamObserver;

/**
 * Stores the state of the crawl in an ES index
 **/

public class ESService extends AbstractFrontierService implements Closeable {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ESService.class);

	private RestHighLevelClient client;

	private final Map<String, Queue> queues = Collections.synchronizedMap(new LinkedHashMap<String, Queue>());

	private String es_index;

	public ESService(JsonNode configurationNode) {

		// connect to ES
		String es_host = "localhost";
		JsonNode tempNode = configurationNode.get("es.host");
		if (tempNode != null && !tempNode.isNull()) {
			es_host = tempNode.asText(es_host);
		}

		int es_port = 9200;
		tempNode = configurationNode.get("es.port");
		if (tempNode != null && !tempNode.isNull()) {
			es_port = tempNode.asInt(es_port);
		}

		es_index = "status";
		tempNode = configurationNode.get("es.index");
		if (tempNode != null && !tempNode.isNull()) {
			es_index = tempNode.asText(es_index);
		}

		client = new RestHighLevelClient(RestClient.builder(new HttpHost(es_host, es_port, "http")));

		// check whether the index exists, if not create it
		GetIndexRequest request = new GetIndexRequest(es_index);
		boolean exists = false;
		boolean success = false;
		while (!success) {
			try {
				exists = client.indices().exists(request, RequestOptions.DEFAULT);
				success = true;
			} catch (IOException e) {
				int waitTime = 10000;
				LOG.error("Error connecting to ES {} {} - trying again in {} secs ", es_host, es_port, waitTime / 1000);
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		}

		// doesn't exist? let's create it
		if (!exists) {
			CreateIndexRequest cir = new CreateIndexRequest(es_index);
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("status-schema.json");
			java.lang.String json = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
					.collect(Collectors.joining("\n"));
			cir.source(json, XContentType.JSON);
			try {
				CreateIndexResponse createIndexResponse = client.indices().create(cir, RequestOptions.DEFAULT);
				boolean ack = createIndexResponse.isAcknowledged();
				LOG.info("Created index definition succesfully: {}", ack);
			} catch (IOException e) {
				LOG.error("Error creating index definition", e);
			}

		}

		// find a way of loading all the known queues in memory...

	}

	@Override
	public void getURLs(GetParams request, StreamObserver<URLInfo> responseObserver) {
		// TODO Auto-generated method stub
		super.getURLs(request, responseObserver);
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

					// TODO

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

		Collection<Queue> _queues = queues.values();

		// specific queue?
		if (request.getValue().isEmpty()) {
			Queue q = queues.get(request.getValue());
			if (q != null) {
				_queues = new LinkedList<>();
				_queues.add(q);
			} else {
				// TODO notify an error to the client
			}
		}

		long now = Instant.now().getEpochSecond();

		for (Queue q : _queues) {
			// inProc += q.getInProcess(now);
			// numQueues++;
			// size += q.size();
			// completed += q.getCountCompleted();
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
	public void listQueues(Integer request, StreamObserver<StringList> responseObserver) {
		long maxQueues = request.getValue();
		// 0 by default
		if (maxQueues == 0) {
			maxQueues = Long.MAX_VALUE;
		}

		LOG.info("Received request to list queues [max {}]", maxQueues);

		long now = Instant.now().getEpochSecond();
		int num = 0;
		Builder list = StringList.newBuilder();

		Iterator<Entry<String, Queue>> iterator = queues.entrySet().iterator();
		while (iterator.hasNext() && num <= maxQueues) {
			Entry<String, Queue> e = iterator.next();
			// check that it isn't blocked
			if (e.getValue().getBlockedUntil() >= now) {
				continue;
			}

			// TODO check that the queue has URLs due for fetching
			// emits a query to ES - very slow...
			list.addValues(e.getKey());
			num++;
		}
		responseObserver.onNext(list.build());
		responseObserver.onCompleted();
	}

	@Override
	public void deleteQueue(crawlercommons.urlfrontier.Urlfrontier.String request,
			StreamObserver<Empty> responseObserver) {
		final String key = request.getValue();

		Queue queue = queues.get(key);
		if (queue == null) {
			responseObserver.onNext(Empty.getDefaultInstance());
			responseObserver.onCompleted();
		}

		// mark the queue object as unavailable for ever
		queue.setBlockedUntil(Long.MAX_VALUE);

		// send a non blocking deletion query to ES

		DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(es_index);
		client.deleteByQueryAsync(deleteByQueryRequest, RequestOptions.DEFAULT,
				new ActionListener<BulkByScrollResponse>() {

					@Override
					public void onResponse(BulkByScrollResponse response) {
						long deleted = response.getDeleted();
						// success?
						LOG.info("{} entries deleted for key {}", deleted, key);
						// once the deletion has completed remove the queue
						queues.remove(key);
					}

					@Override
					public void onFailure(Exception e) {
						// TODO tell the client that a failure occurred
					}

				});

		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void blockQueueUntil(BlockQueueParams request, StreamObserver<Empty> responseObserver) {
		Queue queue = queues.get(request.getKey());
		if (queue != null) {
			queue.setBlockedUntil(request.getTime());
		}
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void setDelay(QueueDelayParams request, StreamObserver<Empty> responseObserver) {
		java.lang.String key = request.getKey();
		if (key.isEmpty()) {
			setDefaultDelayForQueues(request.getDelayRequestable());
		} else {
			Queue queue = queues.get(request.getKey());
			if (queue != null) {
				queue.setDelay(request.getDelayRequestable());
			}
		}
	}

	@Override
	public void close() throws IOException {
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				LOG.error("Closing ", e);
			}
		}

	}

}
