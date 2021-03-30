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

import java.io.Closeable;
import java.io.IOException;

import org.elasticsearch.client.Client;
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

	private Client client;

	public ESService(JsonNode configurationNode) {

		// java.lang.String url =
		// configurationNode.get("h2.connection.url").asText("jdbc:h2:./frontier");

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
