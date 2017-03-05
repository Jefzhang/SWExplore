package crawler.exception;

/**
 * Created by jfzhang on 04/03/2017.
 */
public class PageExceedSizeException extends Exception{
    long pageSize;

    public PageExceedSizeException(long pageSize) {
        super("Aborted fetching of this URL as it's size ( " + pageSize +
                " ) exceeds the maximum size");
        this.pageSize = pageSize;
    }

    public long getPageSize() {
        return pageSize;
    }

}
