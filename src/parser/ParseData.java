package parser;

/**
 * Created by jfzhang on 03/03/2017.
 */
import url.WebURL;
import java.util.Set;
public interface ParseData {

    Set<WebURL> getOutgoingUrls();

    void setOutgoingUrls(Set<WebURL> outgoingUrls);
    boolean isCharacter();



    @Override
    String toString();
}
