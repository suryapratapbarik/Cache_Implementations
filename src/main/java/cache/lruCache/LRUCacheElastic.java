package cache.lruCache;

import cache.CacheInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe, generic Least Recently Used (LRU) cache implementation.
 * It uses a ConcurrentHashMap for thread-safe key-value lookups,
 * ReentrantLock to protect the ordering logic (the doubly linked list),
 * and a dedicated inner DoublyLinkedList class to manage node ordering -
 * improving code structure and readability.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class LRUCacheElastic<K,V> implements CacheInterface<K, V>, AutoCloseable {

    /**
     * Inner class for the nodes of the doubly linked list.
     * It's static to prevent it from holding an implicit reference to the outer lruCache.LRUCache instance.
     */
    private static class Node<K,V> { //Generic
        K key; //Generic
        V value; //Generic
        Node<K,V> prev;
        Node<K,V> next;

        public Node(K key, V val) { //Generic
            this.key = key;
            this.value = val;
        }
    }

    /**
     * Inner class for a doubly linked list to maintain node recency
     * Head of the list is the most recently used item
     * Tail of the list is the least recently used item
     */
    private static class DoublyLinkedList<K,V> { //Generic
        private final Node<K,V> head;
        private final Node<K,V> tail;

        //Initilize sentinel nodes with nulls, as they don't store real data.
        DoublyLinkedList() { //Generic
            this.head = new Node<>(null, null);
            this.tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
        }

        /**
         * Removes an existing node.
         * This must be called only when the lock is held.
         */
        private void remove(Node<K,V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        /**
         * Removes the node at the tail of the list.
         * This must be called only when the lock is held.
         */
        private Node<K, V> removeLast() {
            if (head.next == tail) return null;
            Node<K, V> nodeToRemove = tail.prev;
            remove(nodeToRemove);
            return nodeToRemove;
        }

        /**
         * Adds a new node to the head of the list.
         * This must be called only when the lock is held.
         */
        private void addFirst(Node<K, V> node) { // 4 operations for 4 pointers
            node.next = head.next;
            node.prev = head;
            node.next.prev = node;
            head.next = node;
        }
    }

    // --- Configuration for elasticity ---
    private static final double RESIZE_THRESHOLD_UPPER = 0.8;
    private static final double RESIZE_THRESHOLD_LOWER = 0.25;
    private static final double GROWTH_FACTOR = 1.5;
    private static final double SHRINK_FACTOR = 0.5;
    private static final int MAINTENANCE_PERIOD_SECONDS = 2;

    private final int initialCapacity;
    private final int maxCapacity;
    private volatile int currentCapacity;
    private volatile int size;

    private final DoublyLinkedList<K,V> dlist;
    // Use ConcurrentHashMap for its high-performance, thread-safe operations.
    private final Map<K, Node<K,V>> map; //Generic
    // A single lock to protect all modifications to the linked list structure.
    private final ReentrantLock lock;
    private final ScheduledExecutorService maintenanceExecutor;


    public LRUCacheElastic(int initialCapacity, int maxCapacity) {
        if (initialCapacity <= 0) { // edge case
            throw new IllegalArgumentException("Capacity must be positive.");
        }
        if (maxCapacity < initialCapacity) {
            throw new IllegalArgumentException("Maximum capacity must be >= initialCapacity.");
        }
        this.initialCapacity = initialCapacity;
        this.maxCapacity = maxCapacity;
        this.currentCapacity = initialCapacity;
        this.size = 0;

        this.dlist = new DoublyLinkedList<>();
        this.map = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();

        // Initialize and schedule background maintenance thread
        this.maintenanceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
           Thread t = new Thread(r, "cache-elasticity-manager");
           t.setDaemon(true); // Allow JVM to exit even if this thread is running
            return t;
        });
        this.maintenanceExecutor.scheduleAtFixedRate(this::runElasticityManager,MAINTENANCE_PERIOD_SECONDS,
                MAINTENANCE_PERIOD_SECONDS, TimeUnit.SECONDS);

    }

    /**
     * Core logic of background thread that maintains cache size elasticity
     */
    private void runElasticityManager() {
        lock.lock();
        try {
            double fillRatio = (double)size / currentCapacity;
            if (fillRatio > RESIZE_THRESHOLD_UPPER) {
                int newCapacity = (int) (currentCapacity * GROWTH_FACTOR);
                this.currentCapacity = Math.min(newCapacity, maxCapacity);
                System.out.println("Cache Upsized. New Capacity : "+this.currentCapacity);
            } else if (fillRatio < RESIZE_THRESHOLD_LOWER) {
                int newCapacity = (int) (currentCapacity * SHRINK_FACTOR);
                this.currentCapacity = Math.max(newCapacity, initialCapacity);
                System.out.println("Cache downsized. New Capacity : "+this.currentCapacity);

                // If new capacity is less than current size, we might need to evict some items
                evictUntilWithinCapacity();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Helper method to evict items when downsizing cache.
     */
    private void evictUntilWithinCapacity() {
        while (this.size > this.currentCapacity) {
            Node<K,V> node = dlist.removeLast();
            if (node != null) {
                map.remove(node);
                this.size--;
            } else {
                break; // Should not happen in normal operations
            }
        }
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
            Node<K,V> node = map.get(key); //cast to <K,V>
            dlist.remove(node); //lock protects this modification
            dlist.addFirst(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Inserts or updates a key-value pair. If inserting a new key causes the cache
     * to exceed its capacity, the least recently used element is evicted.
     *
     * @param key   the key with which the specified value is to be associated
     * @param val the value to be associated with the specified key
     */
    public void put(K key, V val) { //Generic
        lock.lock();
        try {
            if (map.containsKey(key)) {
                Node<K,V> node = map.get(key);
                if (node.value != val) {
                    node.value = val;
                }
                dlist.remove(node); //lock protects this modification
                dlist.addFirst(node);
            } else {
                Node<K,V> newNode = new Node(key, val);
                map.put(key, newNode);
                dlist.addFirst(newNode); //lock protects this modification
                size++;
                if (size > currentCapacity) {
                    Node<K,V> node = dlist.removeLast(); //lock protects this modification
                    map.remove(node.key);
                    size--;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        maintenanceExecutor.shutdown();
        try {
            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            maintenanceExecutor.shutdownNow();
            /* Restore the interrupted status of the current thread after it was cleared by JVM on an
            InterruptedException, preserving the interruption signal for higher-level code.
             */
            Thread.currentThread().interrupt();
        }
    }
}