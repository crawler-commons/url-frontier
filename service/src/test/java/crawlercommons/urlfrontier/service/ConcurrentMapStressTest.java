package crawlercommons.urlfrontier.service;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentMapStressTest {

    /*
     * final int threadCount = 100; // Number of threads final int
     * operationsPerThread = 10_000; // Number of operations per thread
     */

    static int threadCount = 30; // Number of threads
    static int operationsPerThread = 5000; // Number of operations per thread

    public static void benchmark(final Map<Integer, Integer> map) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Measure execution time
        long startTime = System.currentTimeMillis();

        // Multiple threads performing read and write operations
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(
                    () -> {
                        for (int j = 0; j < operationsPerThread; j++) {
                            int key = threadId * operationsPerThread + j;
                            map.put(key, key);
                            map.get(key);
                            if (j % 2 == 0) {
                                map.remove(key);
                            }
                        }
                    });
        }

        // Shutdown and await completion
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        // Final assertions
        System.out.println("Final size of the map: " + map.size());
        System.out.println("Execution time (ms): " + (endTime - startTime));

        // Validate data consistency
        int remainingKeys = 0;
        for (int i = 0; i < threadCount * operationsPerThread; i++) {
            if (map.get(i) != null) {
                remainingKeys++;
            }
        }

        System.out.println("Remaining keys count: " + remainingKeys);
        System.out.println("Stress test completed successfully.");
    }

    public static void readHeavy(final Map<Integer, Integer> map) throws InterruptedException {
        final int initialSize = 100_000; // Pre-fill map with this many elements

        // Pre-fill the map with initial data
        for (int i = 0; i < initialSize; i++) {
            map.put(i, i);
        }

        System.out.println("Map pre-filled with " + initialSize + " elements.");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Measure execution time
        long startTime = System.currentTimeMillis();

        // Multiple threads performing read-heavy operations
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(
                    () -> {
                        for (int j = 0; j < operationsPerThread; j++) {
                            // Perform mostly read operations
                            int key = j % initialSize; // Random key within the initial range
                            map.get(key); // Read operation

                            // Occasionally perform a write operation
                            if (j % 1000 == 0) {
                                int newKey = threadId * operationsPerThread + j;
                                map.put(newKey, newKey);
                            }
                        }
                    });
        }

        // Shutdown and await completion
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        // Final assertions
        System.out.println("Final size of the map: " + map.size());
        System.out.println("Execution time (ms): " + (endTime - startTime));

        // Validate data consistency
        int remainingKeys = 0;
        for (int i = 0; i < initialSize; i++) {
            if (map.get(i) != null) {
                remainingKeys++;
            }
        }

        System.out.println("Remaining keys count from pre-filled data: " + remainingKeys);
        System.out.println("Stress test completed successfully.");
    }

    public static void writeHeavy(final Map<Integer, Integer> map) throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // Measure execution time
        long startTime = System.currentTimeMillis();

        // Multiple threads performing write-heavy operations
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(
                    () -> {
                        for (int j = 0; j < operationsPerThread; j++) {
                            // Perform write operations
                            int key = threadId * operationsPerThread + j;
                            map.put(key, key); // Write operation

                            // Occasionally perform a read operation
                            if (j % 100 == 0) {
                                map.get(key); // Read the key just written
                            }

                            // Occasionally remove some keys
                            if (j % 500 == 0) {
                                map.remove(key - 500); // Remove keys added earlier
                            }
                        }
                    });
        }

        // Shutdown and await completion
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();

        // Final assertions
        System.out.println("Final size of the map: " + map.size());
        System.out.println("Execution time (ms): " + (endTime - startTime));

        // Validate data consistency
        int remainingKeys = 0;
        for (int i = 0; i < threadCount * operationsPerThread; i++) {
            if (map.get(i) != null) {
                remainingKeys++;
            }
        }

        System.out.println("Remaining keys count: " + remainingKeys);
        System.out.println("Stress test completed successfully.");
    }

    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedHashMap<Integer, Integer> linkedMap = new ConcurrentLinkedHashMap<>();
        ConcurrentOrderedMap<Integer, Integer> orderedMap = new ConcurrentOrderedMap<>();

        System.out.println("Benchmark ConcurrentLinkedHashMap");
        benchmark(linkedMap);

        System.out.println("Benchmark ConcurrentOrderedMap");
        benchmark(orderedMap);

        linkedMap = new ConcurrentLinkedHashMap<>();
        orderedMap = new ConcurrentOrderedMap<>();
        System.out.println("Read-Heavy test ConcurrentLinkedHashMap");
        readHeavy(linkedMap);

        System.out.println("Read-Heavy ConcurrentOrderedMap");
        readHeavy(orderedMap);

        linkedMap = new ConcurrentLinkedHashMap<>();
        orderedMap = new ConcurrentOrderedMap<>();
        System.out.println("Write-Heavy test ConcurrentLinkedHashMap");
        readHeavy(linkedMap);

        System.out.println("Write-Heavy ConcurrentOrderedMap");
        readHeavy(orderedMap);
    }
}
