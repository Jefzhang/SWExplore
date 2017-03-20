package multithread;

import url.WebURL;
import util.LockFreeQueue;

import java.util.List;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class WebUrlQueues {

    protected LockFreeQueue<WebURL> urlQueue;
    protected boolean isfinished;
    protected Object waitList = new Object();

    public WebUrlQueues(){
        urlQueue = new LockFreeQueue();
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
                result.add(urlQueue.take());
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

    public int getScheduledPageNum(){
        return this.urlQueue.getPageNum();
    }
}
