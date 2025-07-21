package runner;

import lruCache.LRUCache;
import lruCache.LRUCacheInterface;

public class Application {
    public static void main(String args[]) {

        numbers();
        fruits();
    }

    private static void fruits() {
        // Use Parameterized type for Generic classes, avoid Raw use to avoid unchecked compiler warnings, Runtime error: ClassCastException
        LRUCacheInterface<Integer, String> lruCache = new LRUCache<>(2);
        System.out.println("Cache created with capacity 2.");

        lruCache.put(1, "Apple");
        System.out.println("put(1, \"Apple\")");
        lruCache.put(2, "Banana");
        System.out.println("put(2, \"Banana\")");

        System.out.println("get(1): " + lruCache.get(1)); // returns "Apple", moves 1 to front

        lruCache.put(3, "Cherry"); // Evicts key 2 (Banana)
        System.out.println("put(3, \"Cherry\") -> Evicts key 2");

        System.out.println("get(2): " + lruCache.get(2)); // returns null (not found)

        lruCache.put(4, "Date"); // Evicts key 1 (Apple)
        System.out.println("put(4, \"Date\") -> Evicts key 1");

        System.out.println("get(1): " + lruCache.get(1)); // returns null (not found)
        System.out.println("get(3): " + lruCache.get(3)); // returns "Cherry"
        System.out.println("get(4): " + lruCache.get(4)); // returns "Date"
    }

    private static void numbers() {
        // Use Parameterized type for Generic classes, avoid Raw use to avoid unchecked compiler warnings, Runtime error: ClassCastException
        LRUCacheInterface<Integer, Integer> lRUCache = new LRUCache<>(2);
        lRUCache.put(1, 1); // cache is {1=1}
        lRUCache.put(2, 2); // cache is {1=1, 2=2}
        lRUCache.get(1);    // return 1
        lRUCache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
        lRUCache.get(2);    // returns -1 (not found)
        lRUCache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
        lRUCache.get(1);    // return -1 (not found)
        lRUCache.get(3);    // return 3
        lRUCache.get(4);    // return 4
    }
}
