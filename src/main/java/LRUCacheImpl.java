import java.util.HashMap;
import java.util.Map;

public class LRUCacheImpl implements LRUCache{

    private final int capacity;
    private int size;
    private final Map<Integer, DoublyLinkedNode> map;
    DoublyLinkedNode head;
    DoublyLinkedNode tail;

    public LRUCacheImpl(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        map = new HashMap<>();
        head = new DoublyLinkedNode(0,0);
        tail = new DoublyLinkedNode(0,0);
        head.next = tail;
        tail.prev = head;
    }

    @Override
    public int get(int key) {
        if (!map.containsKey(key)) {
            return -1;
        }
        DoublyLinkedNode node = map.get(key);
        moveToHead(node);
        return node.value;
    }

    private void moveToHead(DoublyLinkedNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        addToHead(node);
    }

    @Override
    public void put(int key, int val) {
        if (map.containsKey(key)) {
            DoublyLinkedNode node = map.get(key);
            node.value = val;
            moveToHead(node);
        } else {
            DoublyLinkedNode newNode = new DoublyLinkedNode(key, val);
            map.put(key, newNode);
            addToHead(newNode);
            size++;
            if (size > capacity) {
                DoublyLinkedNode node = removeFromTail();
                map.remove(node.key);
                size--;
            }
        }
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
        LRUCache lRUCache = new LRUCacheImpl(2);
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

/*

Constraints:

1 <= capacity <= 3000
0 <= key <= 10^4
0 <= value <= 10^5
At most 2 * 10^5 calls will be made to get and put.


Input
["LRUCache", "put", "put", "get", "put", "get", "put", "get", "get", "get"]
[[2], [1, 1], [2, 2], [1], [3, 3], [2], [4, 4], [1], [3], [4]]
Output
[null, null, null, 1, null, -1, null, -1, 3, 4]

Explanation
LRUCache lRUCache = new LRUCache(2);
lRUCache.put(1, 1); // cache is {1=1}
lRUCache.put(2, 2); // cache is {1=1, 2=2}
lRUCache.get(1);    // return 1
lRUCache.put(3, 3); // LRU key was 2, evicts key 2, cache is {1=1, 3=3}
lRUCache.get(2);    // returns -1 (not found)
lRUCache.put(4, 4); // LRU key was 1, evicts key 1, cache is {4=4, 3=3}
lRUCache.get(1);    // return -1 (not found)
lRUCache.get(3);    // return 3
lRUCache.get(4);    // return 4

 */