// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "SetActive", description = "Pause or resume the Frontier")
public class SetActive implements Runnable {

    @ParentCommand private Client parent;

    @Parameters(
            defaultValue = "true",
            paramLabel = "BOOLEAN",
            description = "whether the frontier should return URLs or not")
    private boolean state;

    @Option(
            names = {"-l", "--local"},
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

        crawlercommons.urlfrontier.Urlfrontier.Active.Builder builder =
                crawlercommons.urlfrontier.Urlfrontier.Active.newBuilder();
        builder.setLocal(local);
        builder.setState(state);

        blockingFrontier.setActive(builder.build());

        channel.shutdownNow();
    }
}
