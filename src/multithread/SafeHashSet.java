package multithread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.concurrent.locks.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class SafeHashSet extends UrlDiscovered {

    private static final Logger logger = LoggerFactory.getLogger(SafeHashSet.class);

    private HashSet<String> urlBase;
    private final Lock l;

    public SafeHashSet(){
        this.urlBase = new HashSet<>();
        this.l = new ReentrantLock();
    }


    @Override

    public boolean add(String url){
        l.lock();
        try{
            if(!urlBase.contains(url))
                return urlBase.add(url);
            else return false;
        }finally {
            l.unlock();
        }
    }

    @Override

    public boolean contains(String url){
        l.lock();
        try{
            return urlBase.contains(url);
        }finally {
            l.unlock();
        }
    }

    @Override
    public int size(){
        l.lock();
        try{
            return urlBase.size();
        }finally {
            l.unlock();
        }
    }




}
