// SPDX-FileCopyrightText: 2025 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

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
    Entry<K, V> firstEntry();

    /** Remove & returns the first entry according to insertion order */
    Entry<K, V> pollFirstEntry();

    /**
     * Returns a set containing the keys in this map. The iterator returned by this set is weakly
     * consistent. Remove is not supported by the iterator
     */
    @Override
    Set<K> keySet();

    /**
     * Returns a set containing the mappings in this map. The iterator returned by this set is
     * weakly consistent. Remove is not supported by the iterator
     */
    @Override
    Set<Map.Entry<K, V>> entrySet();

    /**
     * Returns a collection containing the values in this map. The iterator returned by this
     * collection is weakly consistent. Remove is not supported by the iterator
     */
    @Override
    Collection<V> values();
}
