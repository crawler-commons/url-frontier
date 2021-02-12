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

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "ListQueues", description = "Prints out active queues")
public class ListQueues implements Runnable {

	@ParentCommand
	private Client parent;

	@Option(names = { "-n",
			"--number_queues" }, defaultValue = "-1", paramLabel = "NUM", description = "maximum number of queues to return")
	private long maxNumQueues;

	@Override
	public void run() {
		ManagedChannel channel = ManagedChannelBuilder.forAddress(parent.hostname, parent.port).usePlaintext().build();
		URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

		crawlercommons.urlfrontier.Urlfrontier.Integer.Builder builder = crawlercommons.urlfrontier.Urlfrontier.Integer
				.newBuilder();
		if (maxNumQueues > 0) {
			builder.setValue(maxNumQueues);
		}

		StringList list = blockingFrontier.listQueues(builder.build());
		for (int i = 0; i < list.getValuesCount(); i++) {
			System.out.println(list.getValues(i));
		}
		channel.shutdownNow();
	}

}
