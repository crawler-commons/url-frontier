/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons
 * SPDX-License-Identifier: Apache-2.0
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import crawlercommons.urlfrontier.Urlfrontier.KnownURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.AbstractFrontierService;
import crawlercommons.urlfrontier.service.QueueInterface;
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

	// no explicit config
	public RocksDBService() {
		this(new HashMap<String, String>());
	}

	private final ConcurrentHashMap<String, String> queuesBeingDeleted = new ConcurrentHashMap<>();

	public RocksDBService(final Map<String, String> configuration) {

		// where to store it?
		String path = configuration.getOrDefault("rocksdb.path", "./rocksdb");

		LOG.info("RocksDB data stored in {} ", path);

		if (configuration.containsKey("rocksdb.purge")) {
			try {
				Files.walk(Paths.get(path)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
			} catch (IOException e) {
				LOG.error("Couldn't delete path {}", path);
			}
		}

		try (final ColumnFamilyOptions cfOpts = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {

			// list of column family descriptors, first entry must always be default column
			// family
			final List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(
					new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
					new ColumnFamilyDescriptor("queues".getBytes(), cfOpts));

			long start = System.currentTimeMillis();

			try (DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
				rocksDB = RocksDB.open(options, path, cfDescriptors, columnFamilyHandleList);
			}

			long end = System.currentTimeMillis();

			LOG.info("RocksDB loaded in {} msec", end - start);

			recoveryQscan();

			long end2 = System.currentTimeMillis();

			LOG.info("{} queues discovered in {} msec", queues.size(), (end2 - end));

		} catch (RocksDBException e) {
			LOG.error("RocksDB exception ", e);
		}

	}

	/** Resurrects the queues from the tables and does sanity checks **/
	private void recoveryQscan() {
		
		LOG.info("Recovering queues from existing RocksDB");
		
		try (final RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandleList.get(1))) {
			for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
				final String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);
				final int pos = currentKey.indexOf('_');
				final String Qkey = keyDeNormalisation(currentKey.substring(0, pos));
				QueueMetadata queueMD = (QueueMetadata) queues.computeIfAbsent(Qkey, s -> new QueueMetadata());
				queueMD.incrementActive();
			}
		}

		String previousQueueID = null;
		long numScheduled = 0;

		// now get the counts of URLs already finished
		try (final RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandleList.get(0))) {
			for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {
				final String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);
				final int pos = currentKey.indexOf('_');
				final String Qkey = keyDeNormalisation(currentKey.substring(0, pos));

				// changed ID? check that the previous one had the correct values
				if (previousQueueID == null) {
					previousQueueID = Qkey;
				} else if (!previousQueueID.equals(Qkey)) {
					if (queues.get(previousQueueID).countActive() != numScheduled)
						throw new RuntimeException("Incorrect number of active URLs for queue " + previousQueueID);
					previousQueueID = Qkey;
					numScheduled = 0;
				}

				// queue might not exist if it had nothing scheduled for it
				// i.e. all done
				QueueMetadata queueMD = (QueueMetadata) queues.computeIfAbsent(Qkey, s -> new QueueMetadata());

				// check the value - if it is an empty byte array it means that the URL has been
				// processed and is not scheduled
				// otherwise it is scheduled
				boolean done = rocksIterator.value().length == 0;
				if (done) {
					queueMD.incrementCompleted();
				} else {
					// double check the number of scheduled later on
					numScheduled++;
				}
			}
		}
		// check the last key
		if (previousQueueID != null && queues.get(previousQueueID).countActive() != numScheduled) {
			throw new RuntimeException("Incorrect number of active URLs for queue " + previousQueueID);
		}
	}

	@Override
	protected int sendURLsForQueue(QueueInterface queue, String queueID, int maxURLsPerQueue, int secsUntilRequestable,
			long now, StreamObserver<URLInfo> responseObserver) {

		int alreadySent = 0;
		final byte[] prefixKey = (keyNormalisation(queueID) + "_").getBytes(StandardCharsets.UTF_8);
		// scan the scheduling table
		try (final RocksIterator rocksIterator = rocksDB.newIterator(columnFamilyHandleList.get(1))) {
			for (rocksIterator.seek(prefixKey); rocksIterator.isValid() && alreadySent < maxURLsPerQueue; rocksIterator
					.next()) {

				final String currentKey = new String(rocksIterator.key(), StandardCharsets.UTF_8);

				// don't want to split the whole string _ as the URL part is left as is
				final int pos = currentKey.indexOf('_');
				final int pos2 = currentKey.indexOf('_', pos + 1);

				final String keyPart = currentKey.substring(0, pos);
				final String urlPart = currentKey.substring(pos2 + 1);

				// not for this queue anymore?
				if (!queueID.equals(keyPart)) {
					return alreadySent;
				}

				// too early for it?
				long scheduled = Long.parseLong(currentKey.substring(pos + 1, pos2));
				if (scheduled > now) {
					// they are sorted by date no need to go further
					return alreadySent;
				}

				// check that the URL is not already being processed
				if (((QueueMetadata) queue).isHeld(urlPart, now)) {
					continue;
				}

				// this one is good to go
				try {
					responseObserver.onNext(URLInfo.parseFrom(rocksIterator.value()));

					// mark it as not processable for N secs
					((QueueMetadata) queue).holdUntil(urlPart, now + secsUntilRequestable);

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

				// has a queue key been defined? if not use the hostname
				if (Qkey.equals("")) {
					LOG.debug("key missing for {}", url);
					Qkey = provideMissingKey(url);
					if (Qkey == null) {
						LOG.error("Malformed URL {}", url);
						responseObserver.onNext(
								crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
						return;
					}
					// make a new info object ready to return
					info = URLInfo.newBuilder(info).setKey(Qkey).build();
				}
				
				// check that the key is not too long
				if (Qkey.length() > 255) {
					LOG.error("Key too long: {}", Qkey);
					responseObserver.onNext(
							crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
					return;
				}

				byte[] schedulingKey = null;

				final byte[] existenceKey = (keyNormalisation(Qkey) + "_" + url).getBytes(StandardCharsets.UTF_8);

				// is this URL already known?
				try {
					schedulingKey = rocksDB.get(existenceKey);
				} catch (RocksDBException e) {
					LOG.error("RocksDB exception", e);
					// TODO notify the client
					return;
				}

				// already known? ignore if discovered
				if (schedulingKey != null && discovered) {
					responseObserver
							.onNext(crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
					return;
				}

				// ignore this url if the queue is being deleted
				if (queuesBeingDeleted.containsKey(Qkey)) {
					LOG.info("Not adding {} as its queue {} is being deleted", url, Qkey);
					responseObserver
							.onNext(crawlercommons.urlfrontier.Urlfrontier.String.newBuilder().setValue(url).build());
					return;
				}

				// get the priority queue or create one
				QueueMetadata queueMD = (QueueMetadata) queues.computeIfAbsent(Qkey, s -> new QueueMetadata());
				try {
					// known - remove from queues
					// its key in the queues was stored in the default cf
					if (schedulingKey != null) {
						rocksDB.delete(columnFamilyHandleList.get(1), schedulingKey);
						// remove from queue metadata
						queueMD.removeFromProcessed(url);
						queueMD.decrementActive();
					}

					// add the new item
					// unless it is an update and it's nextFetchDate is 0 == NEVER
					if (!discovered && nextFetchDate == 0) {
						// does not need scheduling
						// remove any scheduling key from its value
						schedulingKey = new byte[] {};
						queueMD.incrementCompleted();
					} else {
						// it is either brand new or already known
						// create a scheduling key for it
						schedulingKey = (keyNormalisation(Qkey) + "_" + nextFetchDate + "_" + url)
								.getBytes(StandardCharsets.UTF_8);
						// add to the scheduling
						rocksDB.put(columnFamilyHandleList.get(1), schedulingKey, info.toByteArray());
						queueMD.incrementActive();
					}
					// update the link to its queue
					// TODO put in a batch? rocksDB.write(new WriteOptions(), writeBatch);
					rocksDB.put(columnFamilyHandleList.get(0), existenceKey, schedulingKey);

				} catch (RocksDBException e) {
					LOG.error("RocksDB exception", e);
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

	/**
	 * <pre>
	 ** Delete  the queue based on the key in parameter *
	 * </pre>
	 */
	@Override
	public void deleteQueue(crawlercommons.urlfrontier.Urlfrontier.String request,
			StreamObserver<crawlercommons.urlfrontier.Urlfrontier.Integer> responseObserver) {
		final String Qkey = request.getValue();
		int sizeQueue = 0;

		// is this queue is already being deleted?
		// no need to do it again
		if (queuesBeingDeleted.contains(Qkey)) {
			responseObserver
					.onNext(crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder().setValue(sizeQueue).build());
			responseObserver.onCompleted();
			return;
		}

		queuesBeingDeleted.put(Qkey, null);

		// find the next key by alphabetical order
		String[] array = new String[queues.size()];
		array = queues.keySet().toArray(array);
		Arrays.sort(array);
		boolean wantNext = false;
		byte[] endKey = null;
		for (String s : array) {
			if (wantNext) {
				endKey = (keyNormalisation(s) + "_").getBytes(StandardCharsets.UTF_8);
				break;
			} else if (s.equals(Qkey)) {
				wantNext = true;
			}
		}

		try {
			// what if is is the last one?
			boolean endKeyMustAlsoDie = false;
			if (endKey == null) {
				try (RocksIterator iter = rocksDB.newIterator(columnFamilyHandleList.get(0))) {
					iter.seekToLast();
					if (iter.isValid()) {
						// this is the last known URL
						endKey = iter.key();
						endKeyMustAlsoDie = true;
					}
				}
			}

			if (endKey != null) {
				// delete the ranges in the queues table as well as the URLs already
				// processed
				rocksDB.deleteRange(columnFamilyHandleList.get(1),
						(keyNormalisation(Qkey) + "_").getBytes(StandardCharsets.UTF_8), endKey);
				rocksDB.deleteRange(columnFamilyHandleList.get(0),
						(keyNormalisation(Qkey) + "_").getBytes(StandardCharsets.UTF_8), endKey);
				if (endKeyMustAlsoDie) {
					rocksDB.deleteRange(columnFamilyHandleList.get(1), endKey, endKey);
					rocksDB.delete(columnFamilyHandleList.get(0), endKey);
				}
			}

		} catch (RocksDBException e) {
			LOG.error("RocksDBException", e);
		}

		QueueInterface q = queues.remove(Qkey);
		sizeQueue += q.countActive();
		sizeQueue += q.getCountCompleted();

		queuesBeingDeleted.remove(Qkey);

		responseObserver
				.onNext(crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder().setValue(sizeQueue).build());
		responseObserver.onCompleted();
	}

	/**
	 * underscores being used as separator for the keys, we need to make sure that
	 **/
	private static final String keyNormalisation(String key) {
		return key.replaceAll("_", "%5F");
	}

	private static final String keyDeNormalisation(String key) {
		return key.replaceAll("%5F", "_");
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
