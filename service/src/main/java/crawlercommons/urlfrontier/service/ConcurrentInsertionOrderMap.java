// SPDX-FileCopyrightText: 2025 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Interface for a concurrent map which preserves insertion order and exposes weakly consistent
 * ordered iteration.
 *
 * <p>Per-key operations are atomic. Ordered views are backed by the insertion-order index and are
 * weakly consistent: they may miss keys rotated concurrently, and they do not become atomic if the
 * map instance is wrapped in external {@code synchronized} blocks.
 *
 * @param <K>
 * @param <V>
 */
public interface ConcurrentInsertionOrderMap<K, V> extends ConcurrentMap<K, V> {

    /** Returns the first entry according to insertion order */
    Entry<K, V> firstEntry();

    /** Remove and returns the first entry according to insertion order */
    Entry<K, V> pollFirstEntry();

    /**
     * Atomically moves the first entry to the tail and returns it.
     *
     * <p>The mapping stays present in the value map for the whole operation. Returns {@code null}
     * if the map is empty.
     */
    Entry<K, V> rotateFirstEntry();

    /**
     * Returns a set containing the keys in this map. The iterator returned by this set is weakly
     * consistent. Remove is not supported by the iterator
     */
    @Override
    Set<K> keySet();

    /**
     * Returns a set containing the mappings in this map. The iterator returned by this set is
     * weakly consistent: entries removed by another thread while iterating are skipped rather than
     * reported. Remove is not supported by the iterator
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
