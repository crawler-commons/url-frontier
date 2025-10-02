// SPDX-FileCopyrightText: 2025 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import com.google.common.util.concurrent.Striped;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.LoggerFactory;

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
public class ConcurrentOrderedMap<K, V> implements ConcurrentInsertionOrderMap<K, V> {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ConcurrentOrderedMap.class);

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

    public ConcurrentOrderedMap() {
        this(DEFAULT_CONCURRENCY);
    }

    private final ReentrantLock globalLock = new ReentrantLock();

    public ConcurrentOrderedMap(int stripes) {
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
                valueMap.put(key, new ValueEntry(value, newOrder));
                insertionOrderMap.put(newOrder, key);
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
    public Set<Map.Entry<K, V>> entrySetView() {
        LinkedHashSet<Map.Entry<K, V>> entrySet = new LinkedHashSet<>(valueMap.size());
        insertionOrderMap.forEach(
                (order, key) -> {
                    ValueEntry valueEntry = valueMap.get(key);
                    if (valueEntry != null) {
                        entrySet.add(new AbstractMap.SimpleImmutableEntry<>(key, valueEntry.value));
                    } else {
                        LOG.warn(
                                "Inconsistent state (entrySetView): key {} exists in order map but not in value map",
                                key);
                    }
                });

        return entrySet;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    final Iterator<Entry<Long, K>> orderedIterator =
                            insertionOrderMap.entrySet().iterator();

                    Entry<Long, K> nextEntry = null;

                    @Override
                    public boolean hasNext() {
                        while (nextEntry == null && orderedIterator.hasNext()) {
                            Entry<Long, K> entry = orderedIterator.next();
                            K key = entry.getValue();
                            ValueEntry valueEntry = valueMap.get(key);
                            if (valueEntry != null) {
                                this.nextEntry = entry;
                                return true;
                            }
                        }
                        return nextEntry != null;
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        Entry<Long, K> entry = this.nextEntry;
                        this.nextEntry = null;

                        K key = entry.getValue();
                        ValueEntry valueEntry = valueMap.get(key);
                        if (valueEntry == null) {
                            throw new NoSuchElementException();
                        }

                        return new AbstractMap.SimpleImmutableEntry<>(key, valueEntry.value);
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
            ValueEntry valueEntry = valueMap.get(key);

            if (valueEntry != null) {
                return new AbstractMap.SimpleImmutableEntry<>(key, valueEntry.value);
            } else {
                LOG.error(
                        "Inconsistent state (firstEntry): key {} exists in order map but not in value map",
                        key);
                return null;
            }
        } else {
            return null;
        }
    }

    /*
     * Remove & Returns the first entry according to insertion order
     */
    public synchronized Entry<K, V> pollFirstEntry() {
        K key;

        Entry<Long, K> removed = insertionOrderMap.pollFirstEntry();
        if (removed == null) {
            return null;
        }

        // Get and remove from value map
        key = removed.getValue();

        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            ValueEntry valueEntry = valueMap.remove(key);
            if (valueEntry == null) {
                LOG.error(
                        "Inconsistent state (pollFirstEntry): key {} exists in order map but not in value map",
                        key);
                return null;
            }

            return new AbstractMap.SimpleImmutableEntry<>(key, valueEntry.value);
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
                    valueMap.put(key, new ValueEntry(entry.getValue(), newOrder));
                    insertionOrderMap.put(newOrder, key);
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

                    Entry<Long, K> nextEntry = null;

                    @Override
                    public boolean hasNext() {
                        while (nextEntry == null && orderedIterator.hasNext()) {
                            Entry<Long, K> entry = orderedIterator.next();
                            K key = entry.getValue();
                            if (valueMap.containsKey(key)) {
                                nextEntry = entry;
                                return true;
                            }
                        }
                        return nextEntry != null;
                    }

                    @Override
                    public V next() {
                        Entry<Long, K> entry = nextEntry;
                        nextEntry = null;
                        K key = entry.getValue();
                        ValueEntry valueEntry = valueMap.get(key);
                        if (valueEntry == null) {
                            throw new NoSuchElementException();
                        }
                        return valueEntry.value;
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
