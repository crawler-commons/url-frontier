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

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Client", mixinStandardHelpOptions = true, version = "1.0", subcommands = { ListQueues.class,
		GetStats.class, PutURLs.class, GetURLs.class, SetActive.class, GetActive.class,
		DeleteQueue.class }, description = "Interacts with a URL Frontier from the command line")
public class Client {

	@Option(names = { "-t",
			"--host" }, paramLabel = "STRING", defaultValue = "localhost", description = "URL Frontier hostname (defaults to 'localhost')")
	String hostname;

	@Option(names = { "-p",
			"--port" }, defaultValue = "7071", paramLabel = "NUM", description = "URL Frontier port (default to 7071)")
	int port;

	public static void main(String... args) {
		CommandLine cli = new CommandLine(new Client());
		int exitCode = cli.execute(args);
		System.exit(exitCode);
	}

}
