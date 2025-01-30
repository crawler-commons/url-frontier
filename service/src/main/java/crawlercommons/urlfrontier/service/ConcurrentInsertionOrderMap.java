package crawlercommons.urlfrontier.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Interface for a Concurrent Map which preserves insertion order and allows to retrieve its first
 * element.
 *
 * @param <K>
 * @param <V>
 */
public interface ConcurrentInsertionOrderMap<K, V> extends ConcurrentMap<K, V> {

    /** Returns the first entry according to insertion order */
    Entry<K, V> firsEntry();

    /** Remove & returns the first entry according to insertion order */
    Entry<K, V> pollFirstEntry();

    /**
     * Returns a set containing the keys in this map The set is a snapshot of the map and is NOT
     * backed by it (Calling remove on an iterator of this set will not affect the map)
     */
    @Override
    Set<K> keySet();

    /**
     * Returns a set containing the mappings in this map. The set is a snapshot of the map and is
     * NOT backed by it (Calling remove on an iterator of this set will not affect the map)
     */
    @Override
    Set<Map.Entry<K, V>> entrySet();

    /**
     * Returns a collection containing the values in this map. The collection is a snapshot of the
     * map and is NOT backed by it (Calling remove on an iterator of this set will not affect the
     * map) Insertion order of the map will be preserved in the returned collection.
     */
    @Override
    Collection<V> values();
}
