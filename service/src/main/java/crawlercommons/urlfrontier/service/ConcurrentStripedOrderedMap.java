package crawlercommons.urlfrontier.service;

import com.google.common.util.concurrent.Striped;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Concurrent version of LinkedHashMap Design goal is the same as for ConcurrentHashMap: Maintain
 * concurrent readability (typically method get(), but also iterators and related methods) while
 * minimizing update contention.
 *
 * <p>This implementation is based on ConcurrentSkipListMap for order preservation and Guava Striped
 * locks for concurrency.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentStripedOrderedMap<K, V> implements ConcurrentInsertionOrderMap<K, V> {

    // Main storage for key-value pairs
    private final ConcurrentHashMap<K, ValueEntry> valueMap;

    // Tracks insertion order using a concurrent skip list map
    private final ConcurrentSkipListMap<Long, K> insertionOrderMap;

    // Atomic counter to track insertion order
    private final AtomicLong insertionCounter;

    private static final int DEFAULT_CONCURRENCY = 32;
    private static final int DEFAULT_SIZE = DEFAULT_CONCURRENCY * 16;

    private final Striped<Lock> striped;

    class ValueEntry {
        public ValueEntry(V v, long o) {
            this.value = v;
            this.order = o;
        }

        V value;
        long order;
    }

    public ConcurrentStripedOrderedMap() {
        this(DEFAULT_CONCURRENCY);
    }

    private final ReentrantLock globalLock = new ReentrantLock();

    public ConcurrentStripedOrderedMap(int stripes) {
        this.valueMap = new ConcurrentHashMap<>(DEFAULT_SIZE, 0.75f, stripes);
        this.insertionOrderMap = new ConcurrentSkipListMap<>();
        this.insertionCounter = new AtomicLong(0);
        this.striped = Striped.lock(stripes);
    }

    private Lock getStripe(Object key) {
        return striped.get(Objects.hashCode(key));
    }

    private void lockAllStripes() {
        globalLock.lock();
        try {
            // Lock each stripe
            for (int i = 0; i < striped.size(); i++) {
                striped.get(i).lock();
            }
        } finally {
            globalLock.unlock();
        }
    }

    private void unlockAllStripes() {
        // Unlock each stripe
        for (int i = 0; i < striped.size(); i++) {
            striped.get(i).unlock();
        }
    }

    @Override
    public V put(K key, V value) {

        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            // Check if key already exists
            ValueEntry ventry = valueMap.get(key);
            V oldValue = null;
            if (ventry != null) {
                oldValue = ventry.value;
                ventry.value = value;
            } else {
                long newOrder = insertionCounter.getAndIncrement();
                insertionOrderMap.put(newOrder, key);
                valueMap.put(key, new ValueEntry(value, newOrder));
            }

            return oldValue;
        } finally {
            stripe.unlock();
        }
    }

    @Override
    public V get(Object key) {

        ValueEntry ventry = valueMap.get(key);
        return (ventry != null) ? ventry.value : null;
    }

    @Override
    public V remove(Object key) {

        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            ValueEntry removed = valueMap.remove(key);

            // Remove from insertion order map if value existed
            if (removed != null) {
                insertionOrderMap.remove(removed.order);
            }

            return (removed != null) ? removed.value : null;
        } finally {
            stripe.unlock();
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
        lockAllStripes();

        try {
            valueMap.clear();
            insertionOrderMap.clear();
            insertionCounter.set(0);
        } finally {
            unlockAllStripes();
        }
    }

    // Additional methods for more advanced concurrent operations
    @Override
    public boolean replace(K key, V oldValue, V newValue) {

        Lock stripe = getStripe(key);
        stripe.lock();
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
            stripe.unlock();
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            if (!valueMap.containsKey(key)) {
                return this.put(key, value);
            } else {
                return valueMap.get(key).value;
            }
        } finally {
            stripe.unlock();
        }
    }

    @Override
    public boolean remove(Object key, Object value) {

        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            if (valueMap.containsKey(key) && Objects.equals(valueMap.get(key).value, value)) {
                remove(key);
                return true;
            } else {
                return false;
            }
        } finally {
            stripe.unlock();
        }
    }

    @Override
    public V replace(K key, V value) {

        Lock stripe = getStripe(key);
        stripe.lock();
        try {
            if (valueMap.containsKey(key)) {
                return put(key, value);
            } else {
                return null;
            }
        } finally {
            stripe.unlock();
        }
    }

    /*
     * Returns the first entry according to insertion order
     */
    public Entry<K, V> firstEntry() {
        Entry<Long, K> first = insertionOrderMap.firstEntry();

        if (first != null) {
            K key = first.getValue();

            return new AbstractMap.SimpleImmutableEntry<>(key, valueMap.get(key).value);
        } else {
            return null;
        }
    }

    /*
     * Remove & Returns the first entry according to insertion order
     */
    public Entry<K, V> pollFirstEntry() {

        Entry<Long, K> firstEntry = insertionOrderMap.firstEntry();
        if (firstEntry == null) {
            return null;
        }

        K key = firstEntry.getValue();
        Lock stripe = getStripe(key);
        stripe.lock();
        try {

            // Removes the first key from the order queue
            insertionOrderMap.pollFirstEntry();
            if (key != null) {
                // Get the value and remove the entry from the map
                V value = valueMap.get(key).value;
                valueMap.remove(key);

                return new AbstractMap.SimpleImmutableEntry<>(key, value);
            } else {
                return null;
            }
        } finally {
            stripe.unlock();
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

        return valueMap.values().stream().anyMatch(v -> Objects.equals(value, v.value));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {

        // Lock everything here, instead of lock per key, to evaluate
        lockAllStripes();
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
            unlockAllStripes();
        }
    }

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
