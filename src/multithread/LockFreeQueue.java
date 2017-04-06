package multithread;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class LockFreeQueue<E>extends WebUrlQueues<E>{
    class Node {
        public E data;
        public volatile Node next;

        public Node(E data, Node next) {
            this.data = data;
            this.next = next;
        }
    }

    private final AtomicReference<Node> head, tail;
    private AtomicInteger urlCount;

    public LockFreeQueue(){
        super();
        Node sentinel = new Node(null, null);
        this.head = new AtomicReference<>(sentinel);
        this.tail = new AtomicReference<>(sentinel);
        this.urlCount = new AtomicInteger(0);
    }

    public E poll() {
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

    @Override
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

    @Override
    public void schedule(E data) {
        Node newTail = new Node(data, null);
        Node oldTail = tail.getAndSet(newTail);
        oldTail.next = newTail;
        this.urlCount.getAndIncrement();
    }



    @Override
    public void scheduleAll(Collection<E> list){
        for(E data:list){
            schedule(data);
        }
    }

    @Override
    public void getNextURLs(int max, Collection<E>result){
        for(int i=0;i<max;i++){
            E data = poll();
            if(data!=null) result.add(data);
            else break;
        }
    }

    @Override
    public E getNextURL(){
        return poll();
    }




}
