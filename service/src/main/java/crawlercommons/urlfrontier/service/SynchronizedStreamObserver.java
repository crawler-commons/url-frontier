// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import io.grpc.stub.StreamObserver;

/**
 * A {@link StreamObserver} which provides synchronous access to an underlying {@link
 * StreamObserver}.
 *
 * <p>The underlying {@link StreamObserver} must not be used by any other clients.
 */
public class SynchronizedStreamObserver<V> implements StreamObserver<V> {
    private final StreamObserver<V> underlying;

    private int tokens;

    private volatile boolean cancelled;

    SynchronizedStreamObserver(StreamObserver<V> underlying, int startTokens) {
        this.underlying = underlying;
        this.tokens = startTokens;
    }

    /**
     * Create a new {@link SynchronizedStreamObserver} which will delegate all calls to the
     * underlying {@link StreamObserver}, synchronizing access to that observer.
     */
    public static <V> StreamObserver<V> wrapping(StreamObserver<V> underlying, int startTokens) {
        return new SynchronizedStreamObserver<>(underlying, startTokens);
    }

    public boolean tryTakingToken() {
        synchronized (this) {
            if (cancelled) {
                return false;
            }
            // deactivated
            if (tokens < 0) {
                return true;
            } else if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }
    }

    /**
     * Returns true when no more values will be accepted, either because the call was cancelled or
     * because all the tokens have been taken. Lets producers stop looking for values early.
     */
    public boolean isDone() {
        if (cancelled) {
            return true;
        }
        synchronized (this) {
            return tokens == 0;
        }
    }

    /** Marks the underlying call as cancelled: {@link #tryTakingToken()} fails from now on. */
    public void markCancelled() {
        cancelled = true;
    }

    @Override
    public void onNext(V value) {
        synchronized (this) {
            underlying.onNext(value);
        }
    }

    @Override
    public synchronized void onError(Throwable t) {
        synchronized (this) {
            underlying.onError(t);
        }
    }

    @Override
    public synchronized void onCompleted() {
        synchronized (this) {
            underlying.onCompleted();
        }
    }
}
