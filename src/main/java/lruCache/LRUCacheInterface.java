package lruCache;

public interface LRUCacheInterface<K, V> {
    V get(K key);
    void put(K key, V value);
}
