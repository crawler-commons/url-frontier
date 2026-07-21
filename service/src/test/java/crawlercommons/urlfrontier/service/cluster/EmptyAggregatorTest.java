// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmptyAggregatorTest {

    private static final class RecordingObserver implements StreamObserver<Empty> {
        final List<String> events = new ArrayList<>();

        @Override
        public void onNext(Empty value) {
            events.add("next");
        }

        @Override
        public void onError(Throwable t) {
            events.add("error");
        }

        @Override
        public void onCompleted() {
            events.add("completed");
        }
    }

    @Test
    void allChildrenCompletedProducesSingleSuccess() {
        RecordingObserver downstream = new RecordingObserver();
        EmptyAggregator aggregator = new EmptyAggregator(3, downstream);
        StreamObserver<Empty> c1 = aggregator.newChild();
        StreamObserver<Empty> c2 = aggregator.newChild();
        StreamObserver<Empty> c3 = aggregator.newChild();

        c1.onCompleted();
        c2.onCompleted();
        assertTrue(downstream.events.isEmpty(), "no terminal event before all children complete");
        c3.onCompleted();

        assertEquals(List.of("next", "completed"), downstream.events);
    }

    @Test
    void firstErrorWinsAndLaterCompletionsAreIgnored() {
        RecordingObserver downstream = new RecordingObserver();
        EmptyAggregator aggregator = new EmptyAggregator(2, downstream);
        StreamObserver<Empty> c1 = aggregator.newChild();
        StreamObserver<Empty> c2 = aggregator.newChild();

        c1.onError(new RuntimeException("boom"));
        c2.onCompleted();

        assertEquals(List.of("error"), downstream.events);
    }

    @Test
    void lateErrorAfterCompletionIsIgnored() {
        RecordingObserver downstream = new RecordingObserver();
        EmptyAggregator aggregator = new EmptyAggregator(1, downstream);
        StreamObserver<Empty> c1 = aggregator.newChild();

        c1.onCompleted();
        c1.onError(new RuntimeException("late"));

        assertEquals(List.of("next", "completed"), downstream.events);
    }

    @Test
    void doubleErrorProducesSingleTerminalEvent() {
        RecordingObserver downstream = new RecordingObserver();
        EmptyAggregator aggregator = new EmptyAggregator(2, downstream);

        aggregator.newChild().onError(new RuntimeException("a"));
        aggregator.newChild().onError(new RuntimeException("b"));

        assertEquals(List.of("error"), downstream.events);
    }

    @Test
    void zeroExpectedCompletesImmediately() {
        RecordingObserver downstream = new RecordingObserver();
        new EmptyAggregator(0, downstream);

        assertEquals(List.of("next", "completed"), downstream.events);
    }
}
