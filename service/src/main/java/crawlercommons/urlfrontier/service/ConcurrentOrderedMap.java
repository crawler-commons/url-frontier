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
    private final ConcurrentHashMap<K, ValueEntry> valueMap;

    // Tracks insertion order using a concurrent skip list map
    private final ConcurrentSkipListMap<Long, K> insertionOrderMap;

    // Atomic counter to track insertion order
    private final AtomicLong insertionCounter;

    // Stamped lock for read-write operations
    private final StampedLock lock;

    private static final int DEFAULT_CONCURRENCY = 32;
    private static final int DEFAULT_SIZE = DEFAULT_CONCURRENCY * 16;

    class ValueEntry {
        public ValueEntry(V v, long o) {
            this.value = v;
            this.order = o;
        }

        V value;
        long order;
    }

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
            V oldValue;
            ValueEntry ventry = valueMap.get(key);
            if (ventry != null) {
                oldValue = ventry.value;
                ventry.value = value;
            } else {
                // Add to value map and track insertion order
                oldValue = null;
                long newOrder = insertionCounter.getAndIncrement();
                insertionOrderMap.put(newOrder, key);
                valueMap.put(key, new ValueEntry(value, newOrder));
            }

            return oldValue;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V get(Object key) {
        ValueEntry ventry = valueMap.get(key);
        return (ventry != null) ? ventry.value : null;
    }

    @Override
    public V remove(Object key) {
        long stamp = lock.writeLock();
        try {
            ValueEntry removed = valueMap.remove(key);

            if (removed != null) {
                insertionOrderMap.remove(removed.order);
            }

            return (removed != null) ? removed.value : null;
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

        Set<K> orderedKeys = new LinkedHashSet<>(insertionOrderMap.values());

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                orderedKeys = new LinkedHashSet<>(insertionOrderMap.values());
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
                        .map(key -> new SimpleImmutableEntry<>(key, valueMap.get(key).value))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                orderedEntries =
                        insertionOrderMap.values().stream()
                                .map(
                                        key ->
                                                new SimpleImmutableEntry<>(
                                                        key, valueMap.get(key).value))
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
            if (valueMap.containsKey(key)) {
                ValueEntry ventry = valueMap.get(key);
                if (ventry != null && Objects.equals(ventry.value, oldValue)) {
                    ventry.value = newValue;

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        long stamp = lock.writeLock();
        try {
            if (!valueMap.containsKey(key)) {
                long newOrder = insertionCounter.getAndIncrement();
                insertionOrderMap.put(newOrder, key);
                ValueEntry oldValue = valueMap.put(key, new ValueEntry(value, newOrder));
                return (oldValue != null) ? oldValue.value : null;
            } else {
                return valueMap.get(key).value;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean remove(Object key, Object value) {

        if (valueMap.containsKey(key) && Objects.equals(valueMap.get(key).value, value)) {
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

        return new AbstractMap.SimpleImmutableEntry<>(key, valueMap.get(key).value);
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
                V value = valueMap.get(key).value;
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
                        .map(key -> valueMap.get(key).value)
                        .collect(Collectors.toCollection(ArrayList::new));

        // Validate the read to ensure no concurrent modifications
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                values =
                        insertionOrderMap.values().stream()
                                .map(key -> valueMap.get(key).value)
                                .collect(Collectors.toCollection(ArrayList::new));
            } finally {
                lock.unlockRead(stamp);
            }
        }

        return values;
    }
}
