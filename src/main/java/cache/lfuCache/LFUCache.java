package cache.lfuCache;

import cache.CacheInterface;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
public class LFUCache<K,V> implements CacheInterface<K, V> { //Generic

    /**
     * Inner class for the nodes of the doubly linked list.
     * It's static to prevent it from holding an implicit reference to the outer lruCache.LRUCache instance.
     */
    private static class Node<K,V> { //Generic
        K key; //Generic
        V value; //Generic
        Node<K,V> prev;
        Node<K,V> next;
        int freq; // For LFU cache

        public Node(K key, V val) { //Generic
            this.key = key;
            this.value = val;
            this.freq = 1; //  Default frequency of new node
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
        private int size;

        //Initilize sentinel nodes with nulls, as they don't store real data.
        DoublyLinkedList() { //Generic
            this.head = new Node<>(null, null);
            this.tail = new Node<>(null, null);
            head.next = tail;
            tail.prev = head;
            this.size = 0;
        }

        /**
         * Removes an existing node.
         * This must be called only when the lock is held.
         */
        private void remove(Node<K,V> node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            size--;
        }

        /**
         * Removes the node at the tail of the list.
         * This must be called only when the lock is held.
         */
        private Node<K, V> removeLast() {
            if (size == 0) return null;
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
            size++;
        }
    }

    private final int capacity;
    private int size;
    private final Map<Integer, DoublyLinkedList<K,V>> freqMap;
    // Use ConcurrentHashMap for its high-performance, thread-safe operations.
    private final Map<K, Node<K,V>> nodeMap; //Generic
    // A single lock to protect all modifications to the linked list structure.
    private final ReentrantLock lock;
    private int minFreq;


    public LFUCache(int capacity) {
        if (capacity <= 0) { // edge case
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.size = 0;
        this.freqMap = new ConcurrentHashMap<>();
        this.nodeMap = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.minFreq = 0;

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
        if (!nodeMap.containsKey(key)) {
            return null; // For generic types, null is the standard "not found" return value.
        }
        lock.lock();
        try {
            //Re-check if key is removed while getting the lock
            if (!nodeMap.containsKey(key)) {
                return null;
            }
            Node<K,V> node = nodeMap.get(key); //cast to <K,V>
            updateNodeFrequency(node);
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
            if (nodeMap.containsKey(key)) {
                Node<K,V> node = nodeMap.get(key);
                if (node.value != val) {
                    node.value = val;
                }
                updateNodeFrequency(node);
            } else {
                if (nodeMap.size() >= capacity) {
                    DoublyLinkedList<K,V> dlist = freqMap.get(minFreq);
                    Node<K, V> removedNode = dlist.removeLast();
                    nodeMap.remove(removedNode.key);
                }
                Node<K,V> newNode = new Node(key, val);
                nodeMap.put(key, newNode);
                this.minFreq = 1; // as new node has frequency 1
                freqMap.computeIfAbsent(minFreq, k -> new DoublyLinkedList<>());
                freqMap.get(minFreq).addFirst(newNode);
            }
        } finally {
            lock.unlock();
        }
    }

    private void updateNodeFrequency(Node<K,V> node) {
        int oldFreq = node.freq;
        DoublyLinkedList<K, V> oldList = freqMap.get(oldFreq);
        oldList.remove(node);

        // If the old list is now empty, and it was the minimum frequency, update minimum frequency
        if (oldFreq == minFreq && oldList.size == 0) {
            minFreq++;
        }

        node.freq++;
        DoublyLinkedList<K, V> newList = freqMap.computeIfAbsent(node.freq, k-> new DoublyLinkedList<>());
        newList.addFirst(node);
    }
}