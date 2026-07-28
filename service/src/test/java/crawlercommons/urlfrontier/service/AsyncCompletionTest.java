// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AsyncCompletionTest {

    @Test
    void noTaskCompletesOnTheProducerThread() {
        AtomicInteger completions = new AtomicInteger();
        AsyncCompletion completion = new AsyncCompletion(completions::incrementAndGet);

        completion.noMoreTasks();

        assertEquals(1, completions.get());
    }

    @Test
    void registeredTasksHoldTheCompletionBack() {
        AtomicInteger completions = new AtomicInteger();
        AsyncCompletion completion = new AsyncCompletion(completions::incrementAndGet);

        completion.taskStarted();
        completion.taskStarted();
        completion.noMoreTasks();
        assertEquals(0, completions.get(), "outstanding tasks must keep the completion pending");

        completion.taskDone();
        assertEquals(0, completions.get());

        completion.taskDone();
        assertEquals(1, completions.get(), "the last task to finish must complete");
    }

    @Test
    void tasksFinishingBeforeTheProducerDoNotCompleteEarly() {
        AtomicInteger completions = new AtomicInteger();
        AsyncCompletion completion = new AsyncCompletion(completions::incrementAndGet);

        // a task can finish while the producer is still registering the next ones
        completion.taskStarted();
        completion.taskDone();
        assertEquals(0, completions.get(), "the producer may still register more tasks");

        completion.taskStarted();
        completion.taskDone();
        assertEquals(0, completions.get());

        completion.noMoreTasks();
        assertEquals(1, completions.get());
    }

    @Test
    void completesExactlyOnceUnderConcurrency() throws Exception {
        final int tasks = 64;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            for (int run = 0; run < 200; run++) {
                AtomicInteger completions = new AtomicInteger();
                AsyncCompletion completion = new AsyncCompletion(completions::incrementAndGet);
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(tasks);

                for (int i = 0; i < tasks; i++) {
                    completion.taskStarted();
                    pool.execute(
                            () -> {
                                try {
                                    start.await();
                                    completion.taskDone();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                } finally {
                                    done.countDown();
                                }
                            });
                }

                start.countDown();
                completion.noMoreTasks();

                assertTrue(done.await(10, TimeUnit.SECONDS));
                assertEquals(1, completions.get(), "the action must run exactly once");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
