package multithread;

import java.util.HashSet;

/**
 * Created by jfzhang on 04/04/2017.
 */
public abstract class UrlDiscovered {
    protected HashSet<String> urlBase;

    public UrlDiscovered(){
        urlBase = new HashSet<String>();
    }
    public abstract boolean add(String url);

    public abstract boolean contains(String url);

    public abstract int size();

    public abstract HashSet<String> getURLBase();

}
