package multithread;

import java.util.Collection;

/**
 * Created by jfzhang on 04/03/2017.
 */
public abstract class WebUrlQueues<E> {

    protected boolean isfinished;
    protected Object waitList=new Object();
    public WebUrlQueues(){
        this.isfinished = false;
    }

    public abstract void schedule(E data);
    public abstract void scheduleAll(Collection<E> list);

    public abstract void getNextURLs(int max, Collection<E>result);
    public abstract E getNextURL();
    public abstract boolean isEmpty();

    public boolean isFinished(){
        return this.isfinished;
    }

    public void finish(){
        synchronized (waitList){
            this.isfinished = true;
            waitList.notifyAll();
        }
    }


    /*protected ConcurrentLinkedQueue<WebURL> urlQueue;
    protected boolean isfinished;
    protected Object waitList = new Object();

    public WebUrlQueues(){
        urlQueue = new ConcurrentLinkedQueue<WebURL>();
        this.isfinished = false;
    }

    public void scheduleAll(List<WebURL> urls) {
        for (WebURL url : urls){
            urlQueue.add(url);
        }
    }

    public void schedule(WebURL url) {
        urlQueue.add(url);
    }

    public void getNextURLs(int max, List<WebURL> result) {
        if(isfinished) return;

        for(int i=0;i<max;i++){
            if(!urlQueue.isEmpty()){
                result.add(urlQueue.poll());
            }
            else break;
        }

    }

    public boolean isEmpty() {
        return this.urlQueue.isEmpty();
    }

    public boolean isFinished(){
        return this.isfinished;
    }

    public void finish(){
        synchronized (waitList){
            this.isfinished = true;
            waitList.notifyAll();
        }
    }

    /*public int getScheduledPageNum(){
        return this.urlQueue.getPageNum();
    }*/
}
