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
import crawlercommons.urlfrontier.Urlfrontier.Integer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "DeleteQueue", description = "Delete a queue from the Frontier")
public class DeleteQueue implements Runnable {

	@ParentCommand
	private Client parent;

	@Option(names = { "-k",
			"--key" }, defaultValue = "", paramLabel = "STRING", description = "key for the queue to be deleted")
	private String key;

	@Override
	public void run() {
		ManagedChannel channel = ManagedChannelBuilder.forAddress(parent.hostname, parent.port).usePlaintext().build();

		URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

		crawlercommons.urlfrontier.Urlfrontier.String.Builder builder = crawlercommons.urlfrontier.Urlfrontier.String
				.newBuilder();
		if (key.length() > 0) {
			builder.setValue(key);
		}

		Integer s = blockingFrontier.deleteQueue(builder.build());
		System.out.println(s.getValue() + " URLs deleted");

		channel.shutdownNow();
	}

}
