package crawlercommons.urlfrontier.service;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
public class ConcurrentOrderedMap<K, V> implements ConcurrentInsertionOrderMap<K, V> {
    // Main storage for key-value pairs
    private final ConcurrentHashMap<K, V> valueMap;

    // Tracks insertion order using a concurrent skip list map
    private final ConcurrentSkipListMap<Long, K> insertionOrderMap;

    // Atomic counter to track insertion order
    private final AtomicLong insertionCounter;

    // Stamped lock for read-write operations
    private final StampedLock lock;

    private static final int DEFAULT_CONCURRENCY = 32;
    private static final int DEFAULT_SIZE = DEFAULT_CONCURRENCY * 16;

    public ConcurrentOrderedMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this.valueMap = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
        this.insertionOrderMap = new ConcurrentSkipListMap<>();
        this.insertionCounter = new AtomicLong(0);
        this.lock = new StampedLock();
    }

    public ConcurrentOrderedMap() {
        this(DEFAULT_SIZE, 0.75f, DEFAULT_CONCURRENCY);
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
    /**
     * Insertion order is preserved. The entry set returned is not backed up by the map.
     *
     * @return a linked hash set will all keys
     */
    public Set<K> keySet() {
        // Return entries in insertion order
        long stamp = lock.tryOptimisticRead();

        Set<K> orderedKeys =
                insertionOrderMap.values().stream()
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                orderedKeys =
                        insertionOrderMap.values().stream()
                                .collect(Collectors.toCollection(LinkedHashSet::new));
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return orderedKeys;
    }

    /**
     * Insertion order is preserved. The entry set returned is not backed up by the map.
     *
     * @return a linked hash set will all entries
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        // Return entries in insertion order
        long stamp = lock.tryOptimisticRead();

        Set<Entry<K, V>> orderedEntries =
                insertionOrderMap.values().stream()
                        .map(key -> new SimpleImmutableEntry<>(key, valueMap.get(key)))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                orderedEntries =
                        insertionOrderMap.values().stream()
                                .map(key -> new SimpleImmutableEntry<>(key, valueMap.get(key)))
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
    @Override
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

    @Override
    public V putIfAbsent(K key, V value) {
        long stamp = lock.writeLock();
        try {
            if (!valueMap.containsKey(key)) {
                insertionOrderMap.put(insertionCounter.getAndIncrement(), key);
                return valueMap.put(key, value);
            } else {
                return valueMap.get(key);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean remove(Object key, Object value) {

        if (valueMap.containsKey(key) && Objects.equals(valueMap.get(key), value)) {
            remove(key);
            return true;
        } else {
            return false;
        }
    }

    @Override
    // FIXME: Should be atomic but stamped lock is not reentrant
    public V replace(K key, V value) {

        long stamp = lock.writeLock();
        try {
            if (valueMap.containsKey(key)) {
                return put(key, value);
            } else return null;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /*
     * Returns the first entry according to insertion order
     */
    public Entry<K, V> firsEntry() {
        K key = insertionOrderMap.firstEntry().getValue();

        return new AbstractMap.SimpleImmutableEntry<>(key, valueMap.get(key));
    }

    /*
     * Remove & Returns the first entry according to insertion order
     */
    public Entry<K, V> pollFirstEntry() {

        long stamp = lock.writeLock();
        try {

            // Retrieves and removes the first key from the order queue
            Entry<Long, K> firstEntry = insertionOrderMap.pollFirstEntry();
            K key = firstEntry.getValue();
            if (key != null) {
                // Get the value and remove the entry from the map
                V value = valueMap.get(key);
                valueMap.remove(key);

                return new AbstractMap.SimpleImmutableEntry<>(key, value);
            } else {
                return null;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean isEmpty() {
        return valueMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {

        return valueMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {

        return valueMap.containsValue(value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // TODO Implement this optional operation
        throw new UnsupportedOperationException();
    }

    /** Returns a Collection view of the values contained in this map in insertion order. */
    @Override
    public Collection<V> values() {
        List<V> values;

        // Return entries in insertion order
        long stamp = lock.tryOptimisticRead();

        values =
                insertionOrderMap.values().stream()
                        .map(key -> valueMap.get(key))
                        .collect(Collectors.toCollection(ArrayList::new));

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                values =
                        insertionOrderMap.values().stream()
                                .map(key -> valueMap.get(key))
                                .collect(Collectors.toCollection(ArrayList::new));
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return values;
    }
}
