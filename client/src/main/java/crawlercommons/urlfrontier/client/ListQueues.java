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

import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.Pagination.Builder;
import crawlercommons.urlfrontier.Urlfrontier.QueueList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "ListQueues", description = "Prints out active queues")
public class ListQueues implements Runnable {

    @ParentCommand private Client parent;

    @Option(
            names = {"-n", "--number_queues"},
            defaultValue = "100",
            paramLabel = "NUM",
            description = "maximum number of queues to return (default 100)")
    private int maxNumQueues;

    @Option(
            names = {"-s", "--start"},
            defaultValue = "0",
            paramLabel = "NUM",
            description = "starting position of queue to return (default 0)")
    private int start;

    @Option(
            names = {"-o", "--output"},
            defaultValue = "",
            paramLabel = "STRING",
            description = "output file to dump all the queues")
    private String output;

    @Option(
            names = {"-i", "--include_inactive"},
            defaultValue = "false",
            paramLabel = "BOOLEAN",
            description = "include inactive queues in the results")
    private boolean inactive;

    @Override
    public void run() {
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress(parent.hostname, parent.port)
                        .usePlaintext()
                        .build();
        URLFrontierBlockingStub blockingFrontier = URLFrontierGrpc.newBlockingStub(channel);

        if (output.length() > 0) {

            File f = new File(output);
            f.delete();
            CharSink sink = Files.asCharSink(f, Charset.defaultCharset(), FileWriteMode.APPEND);

            while (true) {

                Builder builder = crawlercommons.urlfrontier.Urlfrontier.Pagination.newBuilder();

                builder.setSize(maxNumQueues);
                builder.setStart(start);
                builder.setIncludeInactive(inactive);

                QueueList queueslisted = blockingFrontier.listQueues(builder.build());

                if (queueslisted.getValuesCount() == 0) break;

                for (int i = 0; i < queueslisted.getValuesCount(); i++) {
                    try {
                        sink.write(queueslisted.getValues(i));
                        sink.write("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                start += maxNumQueues;
            }

        } else {
            Builder builder = crawlercommons.urlfrontier.Urlfrontier.Pagination.newBuilder();

            builder.setSize(maxNumQueues);
            builder.setStart(start);
            builder.setIncludeInactive(inactive);

            QueueList queueslisted = blockingFrontier.listQueues(builder.build());

            for (int i = 0; i < queueslisted.getValuesCount(); i++) {
                System.out.println(queueslisted.getValues(i));
            }
        }
        channel.shutdownNow();
    }
}
