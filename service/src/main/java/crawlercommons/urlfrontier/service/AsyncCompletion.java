// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the asynchronous work started on behalf of a single request or stream and runs a
 * completion action exactly once, on whichever thread finishes last.
 *
 * <p>This lets a gRPC handler thread return as soon as it has handed the work over, instead of
 * blocking until the tasks it submitted have finished.
 *
 * <p>The producer (the thread registering the tasks) implicitly holds one unit of outstanding work
 * until it calls {@link #noMoreTasks()}, so the action cannot fire early while tasks are still
 * being registered. Every {@link #taskStarted()} must be matched by exactly one {@link
 * #taskDone()}, including on the paths where the task could not be submitted at all.
 */
public final class AsyncCompletion {

    private final Runnable onCompletion;

    /** outstanding tasks, plus one for the producer until it declares itself done */
    private final AtomicInteger outstanding = new AtomicInteger(1);

    private final AtomicBoolean completed = new AtomicBoolean();

    public AsyncCompletion(Runnable onCompletion) {
        this.onCompletion = onCompletion;
    }

    /** Registers a task about to be handed over to another thread. */
    public void taskStarted() {
        outstanding.incrementAndGet();
    }

    /** Signals that a registered task has finished, whether it succeeded or not. */
    public void taskDone() {
        release();
    }

    /** Signals that no further task will be registered. */
    public void noMoreTasks() {
        release();
    }

    private void release() {
        if (outstanding.decrementAndGet() == 0 && completed.compareAndSet(false, true)) {
            onCompletion.run();
        }
    }
}
