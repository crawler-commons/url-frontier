package crawlercommons.urlfrontier.service;

import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

/**
 * Concurrent version of LinkedHashMap Design goal is the same as for ConcurrentHashMap: Maintain
 * concurrent readability (typically method get(), but also iterators and related methods) while
 * minimizing update contention
 *
 * <p>This implementation is based on StampedLock.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentOrderedMap<K, V> extends AbstractMap<K, V> {
    // Main storage for key-value pairs
    private final ConcurrentHashMap<K, V> valueMap;

    // Tracks insertion order using a concurrent skip list map
    private final ConcurrentSkipListMap<Long, K> insertionOrderMap;

    // Atomic counter to track insertion order
    private final AtomicLong insertionCounter;

    // Stamped lock for read-write operations
    private final StampedLock lock;

    public ConcurrentOrderedMap() {
        this.valueMap = new ConcurrentHashMap<>();
        this.insertionOrderMap = new ConcurrentSkipListMap<>();
        this.insertionCounter = new AtomicLong(0);
        this.lock = new StampedLock();
    }

    @Override
    public V put(K key, V value) {
        long stamp = lock.writeLock();
        try {
            // Check if key already exists
            V oldValue = valueMap.get(key);

            // Remove old insertion order if key exists
            if (oldValue != null) {
                insertionOrderMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
            }

            // Add to value map and track insertion order
            valueMap.put(key, value);
            insertionOrderMap.put(insertionCounter.getAndIncrement(), key);

            return oldValue;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V get(Object key) {
        return valueMap.get(key);
    }

    @Override
    public V remove(Object key) {
        long stamp = lock.writeLock();
        try {
            V removedValue = valueMap.remove(key);

            // Remove from insertion order map if value existed
            if (removedValue != null) {
                insertionOrderMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
            }

            return removedValue;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // Return entries in insertion order
        long stamp = lock.tryOptimisticRead();

        Set<Entry<K, V>> orderedEntries =
                insertionOrderMap.values().stream()
                        .map(key -> new SimpleEntry<>(key, valueMap.get(key)))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                orderedEntries =
                        insertionOrderMap.values().stream()
                                .map(key -> new SimpleEntry<>(key, valueMap.get(key)))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return orderedEntries;
    }

    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public void clear() {
        long stamp = lock.writeLock();
        try {
            valueMap.clear();
            insertionOrderMap.clear();
            insertionCounter.set(0);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // Additional methods for more advanced concurrent operations
    public boolean replace(K key, V oldValue, V newValue) {
        long stamp = lock.writeLock();
        try {
            if (valueMap.replace(key, oldValue, newValue)) {
                // Remove old insertion order entry and add new one
                insertionOrderMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
                insertionOrderMap.put(insertionCounter.getAndIncrement(), key);
                return true;
            }
            return false;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // Example usage:
    public static void main(String[] args) throws InterruptedException {
        ConcurrentLinkedHashMap<Integer, String> clhMap = new ConcurrentLinkedHashMap<>();

        // Simulate concurrent puts
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 1; i <= 20; i++) {
            final int key = i;
            executor.submit(() -> clhMap.put(key, "Value" + key));
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        // Iterate in insertion order
        for (Map.Entry<Integer, String> entry : clhMap.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }
}
