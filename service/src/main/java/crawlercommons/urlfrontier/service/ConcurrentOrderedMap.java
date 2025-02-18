package crawlercommons.urlfrontier.service;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

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
     * Insertion order is preserved.
     *
     * @return a linked hash set will all keys
     */
    public Set<K> keySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<K> iterator() {
                return new Iterator<>() {
                    final Iterator<Entry<Long, K>> orderedIterator =
                            insertionOrderMap.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return orderedIterator.hasNext();
                    }

                    @Override
                    public K next() {
                        return orderedIterator.next().getValue();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                // Don't use size on CSLM as it's not in O(1) but rather O(n) and may be inacurate
                return valueMap.size();
            }
        };
    }

    /**
     * Insertion order is preserved.
     *
     * @return a linked hash set will all entries
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    final Iterator<Entry<Long, K>> orderedIterator =
                            insertionOrderMap.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return orderedIterator.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        Entry<Long, K> nextEntry = orderedIterator.next();
                        K key = nextEntry.getValue();
                        V value = valueMap.get(key).value;
                        return new AbstractMap.SimpleImmutableEntry<>(key, value);
                    }
                };
            }

            @Override
            public int size() {
                return valueMap.size();
            }
        };
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
    public V replace(K key, V value) {

        long stamp = lock.writeLock();
        try {
            if (valueMap.containsKey(key)) {
                ValueEntry vEntry = valueMap.get(key);
                V oldValue = vEntry.value;
                vEntry.value = value;

                return oldValue;
            } else {
                long newOrder = insertionCounter.getAndIncrement();
                insertionOrderMap.put(newOrder, key);
                valueMap.put(key, new ValueEntry(value, newOrder));

                return null;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /*
     * Returns the first entry according to insertion order
     */
    public Entry<K, V> firstEntry() {
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

        long stamp = lock.writeLock();
        try {
            for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
                K key = entry.getKey();

                // Check if key already exists
                ValueEntry ventry = valueMap.get(key);
                if (ventry != null) {
                    ventry.value = entry.getValue();
                } else {
                    long newOrder = insertionCounter.getAndIncrement();
                    insertionOrderMap.put(newOrder, key);
                    valueMap.put(key, new ValueEntry(entry.getValue(), newOrder));
                }
            }
        } finally {
            lock.unlock(stamp);
        }
    }

    /** Returns a Collection view of the values contained in this map in insertion order. */
    @Override
    public Collection<V> values() {
        return new AbstractCollection<>() {
            @Override
            public Iterator<V> iterator() {
                return new Iterator<>() {
                    final Iterator<Entry<Long, K>> orderedIterator =
                            insertionOrderMap.entrySet().iterator();

                    @Override
                    public boolean hasNext() {
                        return orderedIterator.hasNext();
                    }

                    @Override
                    public V next() {
                        K key = orderedIterator.next().getValue();
                        return valueMap.get(key).value;
                    }
                };
            }

            @Override
            public int size() {
                return valueMap.size();
            }
        };
    }
}
