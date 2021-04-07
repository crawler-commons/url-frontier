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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.InvalidProtocolBufferException;

import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
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

		// TODO scan the queues to populate the map of queue metadata

	}

	@Override
	public void getURLs(GetParams request, StreamObserver<URLInfo> responseObserver) {
		// on hold
		if (!isActive()) {
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
			QueueMetadata queue = queues.get(key);

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
				delay = getDefaultDelayForQueues();
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
				Iterator<Entry<String, QueueMetadata>> iterator = queues.entrySet().iterator();
				Entry<String, QueueMetadata> e = iterator.next();
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
					delay = getDefaultDelayForQueues();
				if (e.getValue().getLastProduced() + delay >= now) {
					continue;
				}

				// already has its fill of URLs in process
				if (e.getValue().getInProcess() >= maxURLsPerQueue) {
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
	private int sendURLsForQueue(final QueueMetadata queue, final String queueID, final int maxURLsPerQueue,
			final int secsUntilRequestable, final long now, final StreamObserver<URLInfo> responseObserver) {

		int alreadySent = 0;
		byte[] prefixKey = (queueID + "_").getBytes(StandardCharsets.UTF_8);
		// scan the scheduling table
		try (final RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandleList.get(1))) {
			for (rocksIterator.seek(prefixKey); rocksIterator.isValid() && alreadySent < maxURLsPerQueue; rocksIterator
					.next()) {

				String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);
				String[] splits = currentKey.split("_");

				// not for this queue anymore?
				if (!splits.equals(splits[0])) {
					return alreadySent;
				}

				// too early for it?
				long scheduled = Long.parseLong(splits[1]);
				if (scheduled > now) {
					// they are sorted by date no need to go further
					return alreadySent;
				}

				// check that the URL is not already being processed
				if (queue.isHeld(splits[2], now)) {
					continue;
				}

				// this one is good to go
				try {
					responseObserver.onNext(URLInfo.parseFrom(rocksIterator.value()));

					// mark it as not processable for N secs
					queue.holdUntil(splits[2], Instant.now().plusSeconds(secsUntilRequestable).getEpochSecond());

					alreadySent++;
				} catch (InvalidProtocolBufferException e) {
					LOG.error("Caught unlikely error ", e);
				}
			}
		}

		return alreadySent;
	}

	@Override
	public StreamObserver<URLItem> putURLs(
			StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver) {

		return new StreamObserver<URLItem>() {

			@Override
			public void onNext(URLItem value) {

				long nextFetchDate;
				boolean discovered = true;
				URLInfo info;

				if (value.hasDiscovered()) {
					info = value.getDiscovered().getInfo();
					nextFetchDate = Instant.now().getEpochSecond();
				} else {
					KnownURLItem known = value.getKnown();
					info = known.getInfo();
					nextFetchDate = known.getRefetchableFromDate();
					discovered = Boolean.FALSE;
				}

				String Qkey = info.getKey();
				String url = info.getUrl();

				byte[] schedulingKey = null;

				// is this URL already known?
				try {
					schedulingKey = rocksDB.get(url.getBytes());
				} catch (RocksDBException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}

				// already known? ignore if discovered
				if (schedulingKey != null && discovered) {
					responseObserver
							.onNext(crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
					return;
				}

				// has a queue key been defined? if not use the hostname
				if (Qkey.equals("")) {
					LOG.debug("key missing for {}", url);
					try {
						URL u = new URL(url);
						Qkey = u.getHost();
						// make a new info object ready to return
						info = URLInfo.newBuilder(info).setKey(Qkey).build();
					} catch (MalformedURLException e) {
						LOG.error("Malformed URL {}", url);
						responseObserver.onNext(
								crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
						return;
					}
				}

				// get the priority queue or create one
				QueueMetadata queueMD = queues.computeIfAbsent(Qkey, s -> new QueueMetadata());

				synchronized (queueMD) {
					try {
						// known - remove from queues
						// its key in the queues was stored in the default cf
						if (schedulingKey != null) {
							rocksDB.delete(columnFamilyHandleList.get(1), schedulingKey);
							// remove from queue metadata
							queueMD.addToCompleted(url);
						}

						// add the new item
						// unless it is an update and it's nextFetchDate is 0 == NEVER
						if (!discovered && nextFetchDate == 0) {
							// TODO mark as completed in the queue md
							// does not need scheduling
							// remove any scheduling key from its value
							schedulingKey = new byte[] {};
						} else {
							// it is either brand new or already known
							// create a scheduling key for it
							schedulingKey = (Qkey + "_" + nextFetchDate + "_" + url).getBytes(StandardCharsets.UTF_8);
							// add to the scheduling
							rocksDB.put(columnFamilyHandleList.get(1), schedulingKey, info.toByteArray());
						}
						// update the link to its queue
						// TODO put in a batch? rocksDB.write(new WriteOptions(), writeBatch);
						rocksDB.put(columnFamilyHandleList.get(0), url.getBytes(), schedulingKey);

					} catch (RocksDBException e) {
						LOG.error("RocksDB exception", e);
					}
				}

				responseObserver
						.onNext(crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
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
