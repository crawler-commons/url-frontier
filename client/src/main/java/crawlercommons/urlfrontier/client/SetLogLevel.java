/**
 * SPDX-FileCopyrightText: 2020 Crawler-commons SPDX-License-Identifier: Apache-2.0 Licensed to
 * Crawler-Commons under one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership. DigitalPebble licenses
 * this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams.Builder;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams.Level;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "SetLogLevel",
        description = "Change the log level of a package in the Frontier service")
public class SetLogLevel implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-p", "--package"},
            required = true,
            paramLabel = "STRING",
            description = "package name")
    private String packge;

    @Option(
            names = {"-l", "--level"},
            defaultValue = "DEBUG",
            paramLabel = "STRING",
            description = "Log level [TRACE, DEBUG, INFO, WARN, ERROR]")
    private String level;

    @Option(
            names = {"-c", "--local"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description =
                    "restricts the scope to this frontier instance instead of aggregating over the cluster")
    private Boolean local;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();

        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        Builder builder = crawlercommons.urlfrontier.Urlfrontier.LogLevelParams.newBuilder();
        builder.setLocal(local).setPackage(packge).setLevel(Level.valueOf(level));
        blockingFrontier.setLogLevel(builder.build());

        System.out.println("Set log level for " + packge + " to " + level);

        channel.shutdownNow();
    }
}
