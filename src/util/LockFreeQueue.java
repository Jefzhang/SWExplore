package util;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class LockFreeQueue<E> {
    class Node {
        public E data;
        public volatile Node next;

        public Node(E data, Node next) {
            this.data = data;
            this.next = next;
        }
    }

    private final AtomicReference<Node> head, tail;

    public LockFreeQueue() {
        Node sentinel = new Node(null, null);
        this.head = new AtomicReference<>(sentinel);
        this.tail = new AtomicReference<>(sentinel);
    }

    public boolean isEmpty() {
        Node cur, next;
        do {
            cur = head.get();
            next = cur.next;
            if (next == null || next.data != null)
                break;
        } while (!head.compareAndSet(cur, next));
        return next == null;
    }

    public void add(E data) {
        Node newTail = new Node(data, null);
        Node oldTail = tail.getAndSet(newTail);
        oldTail.next = newTail;
    }

    public E take() {
        Node cur, next;
        do {
            cur = head.get();
            next = cur.next;
            if (next == null)
                return null;
        } while (!head.compareAndSet(cur, next));
        E data = next.data;
        next.data = null;
        return data;
    }


}
