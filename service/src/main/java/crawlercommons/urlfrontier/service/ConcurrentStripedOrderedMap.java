package crawlercommons.urlfrontier.service;

import com.google.common.util.concurrent.Striped;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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
public class ConcurrentStripedOrderedMap<K, V> extends AbstractMap<K, V>
        implements ConcurrentMap<K, V> {

    // Main storage for key-value pairs
    private final ConcurrentHashMap<K, V> valueMap;

    // Tracks insertion order using a concurrent skip list map
    private final ConcurrentSkipListMap<Long, K> insertionOrderMap;

    // Atomic counter to track insertion order
    private final AtomicLong insertionCounter;

    private static final int DEFAULT_STRIPES = 32;

    private final Striped<Lock> striped;

    public ConcurrentStripedOrderedMap() {
        this(DEFAULT_STRIPES);
    }

    private final ReentrantLock globalLock = new ReentrantLock();

    public ConcurrentStripedOrderedMap(int stripes) {
        this.valueMap = new ConcurrentHashMap<>(32 * stripes, 0.75f, stripes);
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
            stripe.unlock();
        }
    }

    @Override
    public V get(Object key) {

        return valueMap.get(key);
    }

    @Override
    public V remove(Object key) {

        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            V removedValue = valueMap.remove(key);

            // Remove from insertion order map if value existed
            if (removedValue != null) {
                insertionOrderMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
            }

            return removedValue;
        } finally {
            stripe.unlock();
        }
    }

    @Override
    /**
     * Insertion order is preserved. The entry set returned is not backed up by the map.
     *
     * @return a linked hash set will all keys
     */
    public Set<K> keySet() {

        // Return keys in insertion order
        Set<K> orderedKeys;

        // Validate the read to ensure no concurrent modification
        orderedKeys =
                insertionOrderMap.values().stream()
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return orderedKeys;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        // Return entries in insertion order

        Set<Entry<K, V>> orderedEntries;

        orderedEntries =
                insertionOrderMap.values().stream()
                        .map(key -> new SimpleImmutableEntry<>(key, valueMap.get(key)))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        return orderedEntries;
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
            if (valueMap.replace(key, oldValue, newValue)) {
                // Remove old insertion order entry and add new one
                insertionOrderMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
                insertionOrderMap.put(insertionCounter.getAndIncrement(), key);
                return true;
            }
            return false;
        } finally {
            stripe.unlock();
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            if (!valueMap.containsKey(key)) return put(key, value);
            else return valueMap.get(key);
        } finally {
            stripe.unlock();
        }
    }

    @Override
    public boolean remove(Object key, Object value) {

        Lock stripe = getStripe(key);
        stripe.lock();

        try {
            if (valueMap.containsKey(key) && Objects.equals(valueMap.get(key), value)) {
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
            if (valueMap.containsKey(key)) return put(key, value);
            else return null;
        } finally {
            stripe.unlock();
        }
    }
}
