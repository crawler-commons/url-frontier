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

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.GetParams;
import crawlercommons.urlfrontier.Urlfrontier.GetParams.Builder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "GetURLs", description = "Get URLs from a Frontier and display in the standard output")
public class GetURLs implements Runnable {

	@ParentCommand
	private Client parent;

	@Option(names = { "-k",
			"--key" }, required = false, paramLabel = "STRING", description = "key to use to target a specific queue")
	private String key;

	@Option(names = { "-d",
			"--delay" }, defaultValue = "0", required = false, paramLabel = "INT", description = "delay before the URLs can be eligible for sending again")
	private int delay;

	@Option(names = { "-m",
			"--max_urls" }, defaultValue = "1", required = false, paramLabel = "INT", description = "max URLs to return per queue")
	private int maxUrls;

	@Option(names = { "-q",
			"--max_queues" }, defaultValue = "1", required = false, paramLabel = "INT", description = "max number of queues")
	private int maxQueues;

	@Override
	public void run() {

		ManagedChannel channel = ManagedChannelBuilder.forAddress(parent.hostname, parent.port).usePlaintext().build();

		URLFrontierBlockingStub stub = URLFrontierGrpc.newBlockingStub(channel);

		Builder request = GetParams.newBuilder().setMaxUrlsPerQueue(maxUrls).setDelayRequestable(delay)
				.setMaxQueues(maxQueues);

		if (key != null && key.length() > 0) {
			request.setKey(key);
		}

		stub.getURLs(request.build()).forEachRemaining(info -> {
			System.out.println(info);
		});

		channel.shutdownNow();
	}

}
