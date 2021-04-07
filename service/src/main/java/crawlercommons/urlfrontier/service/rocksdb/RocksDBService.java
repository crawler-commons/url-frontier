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

package crawlercommons.urlfrontier.service.rocksdb;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import io.grpc.stub.StreamObserver;

public class RocksDBService extends AbstractFrontierService implements Closeable {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RocksDBService.class);

	static {
		RocksDB.loadLibrary();
	}

	private RocksDB rocksDB;

	// a list which will hold the handles for the column families once the db is
	// opened
	private final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

	// in memory map of metadata for each queue
	private Map<String, QueueMetadata> queues = Collections.synchronizedMap(new LinkedHashMap<String, QueueMetadata>());

	public RocksDBService(JsonNode configurationNode) {

		// connect to ES
		String path = "./rocksdb";
		JsonNode tempNode = configurationNode.get("rocksdb.path");
		if (tempNode != null && !tempNode.isNull()) {
			path = tempNode.asText(path);
		}

		try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {

			// list of column family descriptors, first entry must always be default column
			// family
			final List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(
					new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
					new ColumnFamilyDescriptor("queues".getBytes(), cfOpts));

			try (DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
				rocksDB = RocksDB.open(options, path, cfDescriptors, columnFamilyHandleList);
			}
		} catch (RocksDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO scan the queues to populate the map

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

				byte[] urlInfo = null;

				// is this URL already known?
				try {
					urlInfo = rocksDB.get(iu.url.getBytes());
				} catch (RocksDBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// already known? ignore if discovered
				if (urlInfo != null && discovered) {
					responseObserver.onNext(
							crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(iu.url).build());
					return;
				}

				// has a key been defined? if not use the hostname
				if (key.equals("")) {
					LOG.debug("key missing for {}", iu.url);
					try {
						URL u = new URL(iu.url);
						key = u.getHost();
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						responseObserver.onNext(
								crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(iu.url).build());
						return;
					}
				}

				// get the priority queue or create one
				QueueMetadata queueMD = queues.computeIfAbsent(key, s -> new QueueMetadata());

				synchronized (queueMD) {
					// TODO known - remove from queues
					// how to we remember what key it was associated with in the queue?
					if (!discovered) {

					}
					// brand new!
					else {

					}
				}

				// it is either brand new or already known
				// update what we know about it in the store either way
				byte[] urlinfoasbytes = null;
				try {
					rocksDB.put(iu.url.getBytes(), urlinfoasbytes);
				} catch (RocksDBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
	public void close() throws IOException {

		for (final ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleList) {
			columnFamilyHandle.close();
		}

		if (rocksDB != null) {
			try {
				rocksDB.close();
			} catch (Exception e) {
				LOG.error("Closing ", e);
			}
		}

	}

}
