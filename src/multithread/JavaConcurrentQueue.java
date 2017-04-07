package multithread;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by jfzhang on 06/04/2017.
 */
public class JavaConcurrentQueue<E> extends WebUrlQueues<E> {

    private ConcurrentLinkedQueue<E> queue;
    public JavaConcurrentQueue(){
        super();
        queue = new ConcurrentLinkedQueue<E>();
    }
    @Override
    public void schedule(E data){
        queue.add(data);
    }

    @Override
    public void scheduleAll(Collection<E> list){
        queue.addAll(list);
    }

    @Override
    public void getNextURLs(int max, Collection<E>result){
        for(int i=0;i<max;i++){
            E data = queue.poll();
            if(data!=null) result.add(data);
            else break;
        }
    }

    @Override
    public E getNextURL(){
        return queue.poll();
    }

    @Override
    public boolean isEmpty(){
        return queue.isEmpty();
    }

}
