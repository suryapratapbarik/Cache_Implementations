package cache.lruCache;

import cache.CacheInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe, generic Least Recently Used (LRU) cache implementation.
 * It uses a ConcurrentHashMap for thread-safe key-value lookups and a
 * ReentrantLock to protect the ordering logic (the doubly linked list).
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUCacheWithoutEncapsulation<K, V> implements CacheInterface<K, V> { //Generic

    /**
     * Inner class for the nodes of the doubly linked list.
     * It's static to prevent it from holding an implicit reference to the outer lruCache.LRUCache instance.
     */
    private static class DoublyLinkedNode<K, V> { //Generic
        K key; //Generic
        V value; //Generic
        DoublyLinkedNode prev;
        DoublyLinkedNode next;

        public DoublyLinkedNode(K key, V val) { //Generic
            this.key = key;
            this.value = val;
        }
    }


    private final int capacity;
    private int size;
    // Use ConcurrentHashMap for its high-performance, thread-safe operations.
    private final Map<K, DoublyLinkedNode<K, V>> map; //Generic
    // A single lock to protect all modifications to the linked list structure.
    private final ReentrantLock lock;
    // Sentinel nodes to make list operations simpler (avoiding null checks).
    private final DoublyLinkedNode head;
    private final DoublyLinkedNode tail;


    public LRUCacheWithoutEncapsulation(int capacity) {
        if (capacity <= 0) { // edge case
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.size = 0;
        this.map = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        // Initialize sentinel nodes with nulls, as they don't store real data.
        this.head = new DoublyLinkedNode(null, null);
        this.tail = new DoublyLinkedNode(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Retrieves the value for a given key. Returns null if the key is not found.
     * This operation moves the accessed element to the front (most recently used).
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this cache contains no mapping for the key
     */
    public V get(K key) {
        // First, perform a non-blocking check for performance.
        if (!map.containsKey(key)) {
            return null; // For generic types, null is the standard "not found" return value.
        }
        lock.lock();
        try {
            //Re-check if key is removed while getting the lock
            if (!map.containsKey(key)) {
                return null;
            }
            DoublyLinkedNode<K, V> node = map.get(key); //cast to <K,V>
            moveToHead(node); //lock protects this modification
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts or updates a key-value pair. If inserting a new key causes the cache
     * to exceed its capacity, the least recently used element is evicted.
     *
     * @param key the key with which the specified value is to be associated
     * @param val the value to be associated with the specified key
     */
    public void put(K key, V val) { //Generic
        lock.lock();
        try {
            if (map.containsKey(key)) {
                DoublyLinkedNode<K, V> node = map.get(key);
                node.value = val;
                moveToHead(node); //lock protects this modification
            } else {
                DoublyLinkedNode<K, V> newNode = new DoublyLinkedNode(key, val);
                map.put(key, newNode);
                addToHead(newNode); //lock protects this modification
                size++;
                if (size > capacity) {
                    DoublyLinkedNode<K, V> node = removeFromTail(); //lock protects this modification
                    map.remove(node.key);
                    size--;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Moves an existing node to the head of the list.
     * This must be called only when the lock is held.
     */
    private void moveToHead(DoublyLinkedNode<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        addToHead(node);
    }

    /**
     * Removes the node at the tail of the list.
     * This must be called only when the lock is held.
     */
    private DoublyLinkedNode<K, V> removeFromTail() { // 2 pointers of to be removed node ignored
        DoublyLinkedNode<K, V> rem = tail.prev;
        tail.prev = rem.prev;
        rem.prev.next = tail;
        return rem;
    }

    /**
     * Adds a new node to the head of the list.
     * This must be called only when the lock is held.
     */
    private void addToHead(DoublyLinkedNode<K, V> node) { // 4 operations for 4 pointers
        node.next = head.next;
        node.prev = head;
        node.next.prev = node;
        head.next = node;
    }

    public static void main(String args[]) {
        numbers();
        fruits();
    }

    private static void fruits() {
        // Use Parameterized type for Generic classes, avoid Raw use to avoid unchecked compiler warnings, Runtime error: ClassCastException
        LRUCacheWithoutEncapsulation<Integer, String> lruCache = new LRUCacheWithoutEncapsulation<>(2);
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
        LRUCacheWithoutEncapsulation<Integer, Integer> lRUCache = new LRUCacheWithoutEncapsulation<>(2);
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