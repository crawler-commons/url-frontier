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

package crawlercommons.urlfrontier.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.GetParams.Builder;
import crawlercommons.urlfrontier.Urlfrontier.Stats;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class Client {

	public static void main(String[] args) throws InvalidProtocolBufferException {
		int i = 0;
		String arg;

		String host = "localhost";
		int port = 7071;

		List<String> commands = new LinkedList<>();

		while (i < args.length) {
			arg = args[i++];

			if (arg.equals("-host")) {
				if (i < args.length)
					host = args[i++];
				else {
					System.err.println("-host requires a value");
					return;
				}
				continue;
			}

			if (arg.equals("-port")) {
				if (i < args.length)
					port = Integer.parseInt(args[i++]);
				else {
					System.err.println("-port requires a value");
					return;
				}
				continue;
			}

			commands.add(arg);
		}

		ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
		URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

		if (commands.isEmpty()) {
			System.err.println("command missing. Available options are: ListQueues, GetStats, PutURLs.");
			return;
		}

		String command = commands.remove(0);

		if (command.equalsIgnoreCase("ListQueues")) {
			crawlercommons.urlfrontier.Urlfrontier.Integer.Builder builder = crawlercommons.urlfrontier.Urlfrontier.Integer.newBuilder();
			// takes a number as argument			
			if (!commands.isEmpty()) {
				builder.setValue(Long.parseLong(commands.get(0)));
			}
			StringList list = blockingFrontier.listQueues(builder.build());
			for (i = 0; i < list.getValuesCount(); i++) {
				System.out.println(list.getValues(i));
			}
			return;
		}

		if (command.equalsIgnoreCase("GetStats")) {
			crawlercommons.urlfrontier.Urlfrontier.String.Builder builder = crawlercommons.urlfrontier.Urlfrontier.String
					.newBuilder();
			if (!commands.isEmpty()) {
				String key = commands.get(0);
				builder.setValue(key);
			}
			Stats s = blockingFrontier.getStats(builder.build());
			System.out.println("Number of queues: " + s.getNumberOfQueues());
			System.out.println("In process: " + s.getInProcess());
			Map<String, Integer> counts = s.getCountsMap();
			for (Entry<String, Integer> kv : counts.entrySet()) {
				System.out.println(kv.getKey() + " = " + kv.getValue());
			}
			return;
		}

		if (command.equalsIgnoreCase("PutURLs")) {
			// argument is a file
			// read each line and send the content as updates
			if (commands.isEmpty()) {
				System.err.println("File missing");
				return;
			}

			String file = commands.get(0);

			try {
				List<String> allLines = Files.readAllLines(Paths.get(file));

				URLFrontierStub stub = URLFrontierGrpc.newStub(channel);

				final AtomicBoolean completed = new AtomicBoolean(false);
				final AtomicInteger acked = new AtomicInteger(0);

				int sent = 0;

				StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String> responseObserver = new StreamObserver<crawlercommons.urlfrontier.Urlfrontier.String>() {

					@Override
					public void onNext(crawlercommons.urlfrontier.Urlfrontier.String value) {
						// receives confirmation that the value has been received
						acked.addAndGet(1);
					}

					@Override
					public void onError(Throwable t) {
						completed.set(true);
						t.printStackTrace();
					}

					@Override
					public void onCompleted() {
						completed.set(true);
					}
				};

				StreamObserver<URLItem> streamObserver = stub.putURLs(responseObserver);

				int linenum = 0;

				for (String line : allLines) {
					URLItem item = parse(line);
					if (item == null) {
						System.err.println("Invalid input line " + linenum);
					} else {
						streamObserver.onNext(parse(line));
						sent++;
					}
					linenum++;
				}

				streamObserver.onCompleted();

				// wait for completion
				while (completed.get() == false) {
					try {
						Thread.currentThread().sleep(1000);
					} catch (InterruptedException e) {
					}
				}

				System.out.println("Items sent " + sent + " / acked " + acked.get());

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		channel.shutdownNow();

	}

	/**
	 * input format json
	 * 
	 * {url: "http://test.com", key: "test.com", status: "DISCOVERED"}
	 * 
	 * or plain text where each line is a URL and the other fields are left to their
	 * default value i.e. DISCOVERED, no custom metadata, key determined by the
	 * server (i.e. hostname), no explicit nextFetchDate
	 * 
	 * The input file can mix json and text lines.
	 * 
	 **/
	private static URLItem parse(String input) {
		crawlercommons.urlfrontier.Urlfrontier.URLItem.Builder builder = URLItem.newBuilder();
		if (input.trim().startsWith("{")) {
			try {
				JsonFormat.parser().merge(input, builder);
			} catch (InvalidProtocolBufferException e) {
				return null;
			}
		} else {
			builder.setUrl(input.trim());
		}
		return builder.build();
	}

}
