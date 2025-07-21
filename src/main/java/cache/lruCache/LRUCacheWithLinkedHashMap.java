package cache.lruCache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discussion Points: In interview, we would implemented it manually to demonstrate understanding of the underlying data
 * structures (HashMap + Doubly Linked List), but in a production setting, you'd prefer the battle-tested, optimized, and
 * simpler LinkedHashMap solution unless specific performance characteristics of the manual implementation were required.
 * @param <K>
 * @param <V>
 */
public class LRUCacheWithLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCacheWithLinkedHashMap(int capacity) {
        // The 'true' argument enables access-order mode
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        // This method is called after a new entry is inserted
        return size() > capacity;
    }
}

