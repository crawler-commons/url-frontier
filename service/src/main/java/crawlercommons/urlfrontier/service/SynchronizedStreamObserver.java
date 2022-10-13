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
package crawlercommons.urlfrontier.service;

import io.grpc.stub.StreamObserver;

/**
 * A {@link StreamObserver} which provides synchronous access access to an underlying {@link
 * StreamObserver}.
 *
 * <p>The underlying {@link StreamObserver} must not be used by any other clients.
 */
public class SynchronizedStreamObserver<V> implements StreamObserver<V> {
    private final StreamObserver<V> underlying;

    private int tokens;

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
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }
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
