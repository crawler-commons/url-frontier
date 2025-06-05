// SPDX-FileCopyrightText: 2025 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConcurrentOrderedMapTest {

    private ConcurrentOrderedMap<String, String> map;

    private final int NUM_THREADS = 10;
    private final int NUM_ITERATIONS = 100;

    @BeforeEach
    void setUp() {
        map = new ConcurrentOrderedMap<>();
    }

    @Test
    void testPutAndGet() {
        map.put("key1", "value1");
        assertEquals("value1", map.get("key1"), "Value should be retrieved correctly");

        map.put("key2", "value2");
        assertEquals("value2", map.get("key2"), "Value should be retrieved correctly");
    }

    @Test
    void testContainsKey() {
        // Add a key-value pair to the map
        map.put("key1", "value1");

        // Check if the map contains the key
        boolean contains = map.containsKey("key1");
        assertTrue(contains);

        contains = map.containsKey("key2");
        assertFalse(contains);
    }

    @Test
    void testContainsValue() {
        // Add a key-value pair to the map
        map.put("key1", "value1");

        // Check if the map contains the key
        boolean contains = map.containsValue("value1");
        assertTrue(contains);

        contains = map.containsValue("value2");
        assertFalse(contains);
    }

    @Test
    void testRemove() {
        map.put("key1", "value1");
        map.remove("key1");
        assertNull(map.get("key1"), "Key should be removed from the map");

        assertEquals(0, map.size(), "Size of the map should be zero after removal");
    }

    @Test
    void testConcurrentOperations() {
        // This test checks that multiple threads can add and remove entries concurrently

        Thread[] threads = new Thread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = 0; j < NUM_ITERATIONS; j++) {
                                    String key = Thread.currentThread().getId() + " iter=" + j;
                                    String value = "value" + j;
                                    map.put(key, value);
                                    assertEquals(
                                            value,
                                            map.get(key),
                                            "Value should be retrieved correctly");

                                    // Remove the key after adding
                                    map.remove(key);
                                }
                            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        assertEquals(0, map.size(), "Size of the map should be zero after concurrent operations");
    }

    @Test
    void testOrderAndSize() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");

        assertEquals(3, map.size(), "Size of the map should be 3");

        Iterator<String> iterator = map.keySet().iterator();
        assertEquals("key1", iterator.next());
        assertEquals("key2", iterator.next());
        assertEquals("key3", iterator.next());
    }

    @Test
    void testFirstEntry() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");

        assertEquals("key1", map.firstEntry().getKey());
        assertEquals("key1", map.pollFirstEntry().getKey());
        assertEquals("key2", map.firstEntry().getKey());
    }

    @Test
    void testPutExistingKey() {
        // Add a key-value pair to the map
        map.put("key1", "value1");

        // Overwrite the existing key with a new value
        map.put("key1", "new_value1");

        // Retrieve the updated value
        String value = map.get("key1");
        assertEquals("new_value1", value);
    }

    void testPutIfAbsent() {
        map.putIfAbsent("key1", "value1");
        assertEquals("value1", map.get("key1"), "Value should be retrieved correctly");

        String ret = map.putIfAbsent("key1", "value2");
        assertEquals("value1", ret);
    }

    @Test
    void testClear() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertEquals(2, map.size());

        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    void testEntrySet() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(3, entries.size());
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("key1", "value1")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("key2", "value2")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("key3", "value3")));
    }

    @Test
    void testKeySet() {
        map.put("key1", "value1");
        map.put("key2", "value2");

        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    void testValues() {
        map.put("key1", "value1");
        map.put("key2", "value2");

        Collection<String> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
    }

    @Test
    void testIsEmpty() {
        assertTrue(map.isEmpty());

        map.put("key1", "value1");
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    void testPutAll() {
        Map<String, String> otherMap = Map.of("key1", "value1", "key2", "value2");
        map.putAll(otherMap);

        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    void testCompute() {
        map.put("key1", "value1");
        map.compute("key1", (k, v) -> v + " updated");
        assertEquals("value1 updated", map.get("key1"));

        map.compute("key2", (k, v) -> "new_value");
        assertEquals("new_value", map.get("key2"));
    }

    @Test
    void testRemoveWithValues() {
        map.put("key1", "value1");
        assertTrue(map.remove("key1", "value1"));
        assertFalse(map.containsKey("key1"));
    }

    @Test
    void testReplaceAll() {
        map.put("key1", "value1");
        map.put("key2", "value2");

        map.replaceAll((k, v) -> v + "_updated");
        assertEquals("value1_updated", map.get("key1"));
    }

    @Test
    void testForEach() {
        map.put("key1", "value1");
        map.put("key2", "value2");

        int[] count = {0};
        map.forEach((k, v) -> count[0]++);
        assertEquals(2, count[0]);
    }

    @Test
    void testEntrySetIterator() {
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, "value" + i);
        }

        map.remove("key5");

        // key5 will be at the end
        map.put("key5", null);

        Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();

        // Removes key0
        map.pollFirstEntry();

        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();

            // Remove while iterating
            if ("key2".equals(key)) {
                map.remove("key6");
            }

            assertTrue(key.startsWith("key"));
            if ("key5".equals(key)) {
                assertNull(value);
            } else {
                assertTrue(value.startsWith("value"));
            }
        }
    }

    @Test
    void testKeySetIterator() {
        for (int i = 0; i < 10; i++) {
            map.put("key" + i, "value" + i);
        }

        map.remove("key5");

        // key5 will be at the end
        map.put("key5", null);

        map.remove("key6");

        Iterator<String> iterator = map.keySet().iterator();

        // Removes key0
        map.pollFirstEntry();

        while (iterator.hasNext()) {
            String key = iterator.next();

            assertTrue(key.startsWith("key"));
            if ("key0".equals(key)) {
                assertNull(map.get(key));
            } else if ("key5".equals(key)) {
                assertNull(map.get(key));
            } else {
                assertTrue(map.get(key).startsWith("value"));
            }
        }
    }

    @Test
    void testConcurrentPollFirstEntry() throws InterruptedException {
        // This test checks that multiple threads can poll entries from the map concurrently
        // Fill the map with entries
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            map.put("key" + i, "value" + i);
        }

        Set<String> polledKeys = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
        Thread[] threads = new Thread[NUM_THREADS];

        Runnable poller =
                () -> {
                    Map.Entry<String, String> entry;
                    while ((entry = map.pollFirstEntry()) != null) {
                        // Ensure each key is only polled once
                        boolean unique = polledKeys.add(entry.getKey());
                        assertTrue(unique, "Duplicate key polled: " + entry.getKey());
                        assertNotNull(entry.getValue());
                    }
                };

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread(poller);
        }
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }

        // All entries should have been polled exactly once
        assertEquals(NUM_ITERATIONS, polledKeys.size());
        assertTrue(map.isEmpty());
    }
}
