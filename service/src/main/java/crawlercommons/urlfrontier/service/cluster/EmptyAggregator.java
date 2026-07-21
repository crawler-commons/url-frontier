// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import crawlercommons.urlfrontier.Urlfrontier.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aggregates the responses of N unary Empty calls into a single terminal event on the downstream
 * observer: onNext + onCompleted once all N children have completed, or onError on the first child
 * error. Guarantees exactly one terminal event; late child callbacks after termination are ignored.
 */
class EmptyAggregator {

    private final StreamObserver<Empty> downstream;
    private final AtomicInteger pending;
    private final AtomicBoolean terminated = new AtomicBoolean(false);

    EmptyAggregator(int expected, StreamObserver<Empty> downstream) {
        this.downstream = downstream;
        this.pending = new AtomicInteger(expected);
        if (expected <= 0 && terminated.compareAndSet(false, true)) {
            downstream.onNext(Empty.getDefaultInstance());
            downstream.onCompleted();
        }
    }

    StreamObserver<Empty> newChild() {
        return new StreamObserver<Empty>() {
            @Override
            public void onNext(Empty value) {
                // unary Empty payload carries no information
            }

            @Override
            public void onError(Throwable t) {
                if (terminated.compareAndSet(false, true)) {
                    downstream.onError(Status.fromThrowable(t).asRuntimeException());
                }
            }

            @Override
            public void onCompleted() {
                if (pending.decrementAndGet() == 0 && terminated.compareAndSet(false, true)) {
                    downstream.onNext(Empty.getDefaultInstance());
                    downstream.onCompleted();
                }
            }
        };
    }
}
