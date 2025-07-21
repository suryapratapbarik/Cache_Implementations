package lruCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCacheThreadSafeReentrantLock {

    private class DoublyLinkedNode {
        int key;
        int value;
        DoublyLinkedNode prev;
        DoublyLinkedNode next;

        public DoublyLinkedNode(int key, int val) {
            this.key = key;
            this.value = val;
        }
    }


    private final int capacity;
    private int size;
    private final Map<Integer, DoublyLinkedNode> map;
    private final ReentrantLock lock;
    private final DoublyLinkedNode head;
    private final DoublyLinkedNode tail;


    public LRUCacheThreadSafeReentrantLock(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.map = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.head = new DoublyLinkedNode(0,0);
        this.tail = new DoublyLinkedNode(0,0);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        if (!map.containsKey(key)) {
            return -1;
        }
        lock.lock();
        try {
            //Re-check if key is removed while getting the lock
            if (!map.containsKey(key)) {
                return -1;
            }
            DoublyLinkedNode node = map.get(key);
            moveToHead(node); //lock protects this modification
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    public void put(int key, int val) {
        lock.lock();
        try {
            if (map.containsKey(key)) {
                DoublyLinkedNode node = map.get(key);
                node.value = val;
                moveToHead(node); //lock protects this modification
            } else {
                DoublyLinkedNode newNode = new DoublyLinkedNode(key, val);
                map.put(key, newNode);
                addToHead(newNode); //lock protects this modification
                size++;
                if (size > capacity) {
                    DoublyLinkedNode node = removeFromTail(); //lock protects this modification
                    map.remove(node.key);
                    size--;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private void moveToHead(DoublyLinkedNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        addToHead(node);
    }

    private DoublyLinkedNode removeFromTail() { // 2 pointers of to be removed node ignored
        DoublyLinkedNode rem = tail.prev;
        tail.prev = rem.prev;
        rem.prev.next = tail;
        return rem;
    }

    private void addToHead(DoublyLinkedNode node) { // 4 operations for 4 pointers
        node.next = head.next;
        node.prev = head;
        node.next.prev = node;
        head.next = node;
    }

    public static void main(String args[]) {
        LRUCacheThreadSafeReentrantLock lRUCache = new LRUCacheThreadSafeReentrantLock(2);
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