package crawler.exception;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class ContentFetchException extends Exception {
    String url;
    public ContentFetchException(String url){
        super("Failed to fetch page with url: "+url);
        this.url = url;
    }
    public String getUrl(){
        return this.url;
    }
}
