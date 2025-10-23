// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "Client",
        mixinStandardHelpOptions = true,
        version = "2.6-SNAPSHOT",
        subcommands = {
            ListNodes.class,
            ListQueues.class,
            ListCrawls.class,
            ListURLs.class,
            GetStats.class,
            PutURLs.class,
            GetURLs.class,
            SetActive.class,
            GetActive.class,
            DeleteQueue.class,
            DeleteCrawl.class,
            SetLogLevel.class,
            SetCrawlLimit.class,
            GetURLStatus.class,
            CountURLs.class
        },
        description = "Interacts with a URL Frontier from the command line")
public class Client {

    @Option(
            names = {"-t", "--host"},
            paramLabel = "STRING",
            defaultValue = "localhost",
            description = "URL Frontier hostname (defaults to 'localhost')")
    String hostname;

    @Option(
            names = {"-p", "--port"},
            defaultValue = "7071",
            paramLabel = "NUM",
            description = "URL Frontier port (default to 7071)")
    int port;

    public static void main(String... args) {
        CommandLine cli = new CommandLine(new Client());
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }
}
