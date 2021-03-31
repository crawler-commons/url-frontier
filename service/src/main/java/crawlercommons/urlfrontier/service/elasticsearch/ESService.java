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
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.Integer;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.String;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import io.grpc.stub.StreamObserver;

/**
 * Stores the state of the crawl in an ES index
 **/

public class ESService extends AbstractFrontierService implements Closeable {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ESService.class);

	private RestHighLevelClient client;

	public ESService(JsonNode configurationNode) {

		// connect to ES
		java.lang.String es_host = "localhost";
		JsonNode tempNode = configurationNode.get("es.host");
		if (tempNode != null && !tempNode.isNull()) {
			es_host = tempNode.asText(es_host);
		}

		int es_port = 9200;
		tempNode = configurationNode.get("es.port");
		if (tempNode != null && !tempNode.isNull()) {
			es_port = tempNode.asInt(es_port);
		}

		java.lang.String es_index = "status";
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
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("es-schema.json");
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

		// find a way of loading all the known queues in memory

	}

	@Override
	public void listQueues(Integer request, StreamObserver<StringList> responseObserver) {
		// TODO Auto-generated method stub
		super.listQueues(request, responseObserver);
	}

	@Override
	public void getURLs(GetParams request, StreamObserver<URLInfo> responseObserver) {
		// TODO Auto-generated method stub
		super.getURLs(request, responseObserver);
	}

	@Override
	public StreamObserver<URLItem> putURLs(StreamObserver<String> responseObserver) {
		// TODO Auto-generated method stub
		return super.putURLs(responseObserver);
	}

	@Override
	public void getStats(String request, StreamObserver<Stats> responseObserver) {
		// TODO Auto-generated method stub
		super.getStats(request, responseObserver);
	}

	@Override
	public void deleteQueue(String request, StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		super.deleteQueue(request, responseObserver);
	}

	@Override
	public void blockQueueUntil(BlockQueueParams request, StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		super.blockQueueUntil(request, responseObserver);
	}

	@Override
	public void setDelay(QueueDelayParams request, StreamObserver<Empty> responseObserver) {
		// TODO Auto-generated method stub
		super.setDelay(request, responseObserver);
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
