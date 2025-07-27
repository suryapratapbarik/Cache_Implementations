package cache.runner;

import cache.lfuCache.LFUCache;
import cache.lruCache.LRUCacheElastic;
import cache.CacheInterface;

public class Application {
    public static void main(String args[]) throws InterruptedException {

        //LFU
        //fruits(new LFUCache<>(2));
        //numbers(new LFUCache<>(2));

        //LRU
        fruits(new LRUCacheElastic<>(2, 5));
        numbers(new LRUCacheElastic<>(2, 5));
    }

    private static void fruits(CacheInterface<Integer, String> cache) throws InterruptedException {
        // Use Parameterized type for Generic classes, avoid Raw use to avoid unchecked compiler warnings, Runtime error: ClassCastException
        //CacheInterface<Integer, String> lruCache = new LRUCache<>(2);
        System.out.println("Cache created with capacity 2.");

        cache.put(1, "Apple");
        System.out.println("put(1, \"Apple\")");
        cache.put(2, "Banana");
        System.out.println("put(2, \"Banana\")");
        Thread.sleep(5000);

        System.out.println("get(1): " + cache.get(1)); // returns "Apple", moves 1 to front

        cache.put(3, "Cherry"); // Evicts key 2 (Banana)
        System.out.println("put(3, \"Cherry\") -> Evicts key");

        System.out.println("get(2): " + cache.get(2)); // returns null (not found)

        cache.put(4, "Date"); // Evicts key 1 (Apple)
        System.out.println("put(4, \"Date\") -> Evicts key");

        System.out.println("get(1): " + cache.get(1)); // returns null (not found)
        System.out.println("get(3): " + cache.get(3)); // returns "Cherry"
        System.out.println("get(4): " + cache.get(4)); // returns "Date"
    }

    private static void numbers(CacheInterface<Integer, Integer> cache) {
        // Use Parameterized type for Generic classes, avoid Raw use to avoid unchecked compiler warnings, Runtime error: ClassCastException
        cache.put(1, 1); // cache is {1=1}
        cache.put(2, 2); // cache is {1=1, 2=2}
        cache.get(1);    // return 1
        cache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
        cache.get(2);    // returns -1 (not found)
        cache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
        cache.get(1);    // return -1 (not found)
        cache.get(3);    // return 3
        cache.get(4);    // return 4
    }
}
