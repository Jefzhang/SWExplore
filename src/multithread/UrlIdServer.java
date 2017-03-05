package multithread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class UrlIdServer {

    private static final Logger logger = LoggerFactory.getLogger(UrlIdServer.class);

    private HashMap<String,Integer> urlIdMap;
    private int lastId;
    protected final Object mutex = new Object();

    public UrlIdServer(){
        this.urlIdMap = new HashMap<>();
        this.lastId = 0;
    }


    /**
     * Get the unique Id of url, return -1 if this url doesn't yet exist
     * @param url
     * @return the unique Id of url
     */
    public int getUrlId(String url){
        synchronized (mutex){
            if(!urlIdMap.containsKey(url)) return -1;
        }
        return urlIdMap.get(url);
    }

    public int getNewUrlID(String url) {
        synchronized (mutex) {
            try {
                // Make sure that we have not already assigned a docid for this URL
                int docID = getUrlId(url);
                if (docID > 0) {
                    return docID;
                }
                ++lastId;
                urlIdMap.put(url,lastId);
                return lastId;
            } catch (Exception e) {
                logger.error("Exception thrown while getting new DocID", e);
                return -1;
            }
        }
    }

    public void addUrlIdPair(String url, int docId) throws Exception {
        synchronized (mutex) {
            if (docId <= lastId) {
                throw new Exception(
                        "Requested doc id: " + docId + " is not larger than: " + lastId);
            }

            // Make sure that we have not already assigned a docid for this URL
            int prevId = getUrlId(url);
            if (prevId > 0) {
                if (prevId == docId) {
                    return;
                }
                throw new Exception("Doc id: " + prevId + " is already assigned to URL: " + url);
            }

            //docIDsDB.put(null, new DatabaseEntry(url.getBytes()),
                   // new DatabaseEntry(Util.int2ByteArray(docId)));
            lastId = docId;
        }
    }

    public boolean isSeenBefore(String url) {
        return getUrlId(url) != -1;
    }

    public final int getDocCount() {
        return this.urlIdMap.keySet().size();
    }

}
