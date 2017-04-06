package multithread;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
//import multithread.LockFreeQueue.Node;

/**
 * Created by jfzhang on 06/04/2017.
 */
public class SimpleBlockingQueue<E>extends WebUrlQueues<E>{
    class Node {
        public E data;
        public volatile Node next;

        public Node(E data, Node next) {
            this.data = data;
            this.next = next;
        }
    }
    private Node head;
    private Node tail;
    private Lock lock_h;
    private Lock lock_t;

    public SimpleBlockingQueue(){
        super();
        Node sentinel = new Node(null, null);
        head = sentinel;
        tail = sentinel;
        lock_h = new ReentrantLock();
        lock_t = new ReentrantLock();

    }

    public E poll(){
        Node next;
        E data;
        lock_h.lock();
        try{
            next = head.next;
            if(next==null) return null;
            data = next.data;
            head = next;
        }finally {
            lock_h.unlock();
        }
        return data;
    }


    @Override
    public void schedule(E data){
        Node newTail = new Node(data,null);
        lock_t.lock();
        try{
            tail.next = newTail;
            tail = newTail;
        }finally {
            lock_t.unlock();
        }
    }

    @Override
    public void scheduleAll(Collection<E> list){
        lock_t.lock();
        try{
            for(E data:list){
                Node newTail = new Node(data,null);
                tail.next = newTail;
                tail = newTail;
            }
        }finally {
            lock_t.unlock();
        }
    }

    @Override
    public void getNextURLs(int max, Collection<E>result){
        Node next;
        lock_h.lock();
        try {
            for (int i = 0; i < max; i++) {
                next = head.next;
                if (next != null) {
                    result.add(next.data);
                    head = next;
                } else {
                    break;
                }
            }
        }finally {
            lock_h.unlock();
        }
    }

    @Override
    public E getNextURL(){
        return poll();
    }





    public boolean isEmpty(){
        lock_h.lock();
        try{
            return (head.next==null);
        }finally {
            lock_h.unlock();
        }
    }

}
