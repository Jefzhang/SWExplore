package url;

import java.io.Serializable;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * Created by jfzhang on 03/03/2017.
 */

@Entity
public class WebURL implements Serializable{
    private static final long serialVersionUID = 1L;

    @PrimaryKey
    private String url;

    //private int docid;
    //private int parentDocid;
   // private String parentUrl;
    private String parentAnchor;
    private short depth;
    private String domain;
    private String subDomain;
    private String path;
    private String anchor;
    private byte priority;
   // private String tag;



    /**
     * @return Url string
     */
    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;

        int domainStartIdx = url.indexOf("//") + 2;
        int domainEndIdx = url.indexOf('/', domainStartIdx);
        domainEndIdx = (domainEndIdx > domainStartIdx) ? domainEndIdx : url.length();
        domain = url.substring(domainStartIdx, domainEndIdx);
        subDomain = "";
        String[] parts = domain.split("\\.");
        if (parts.length > 2) {
            domain = parts[parts.length - 2] + "." + parts[parts.length - 1];
            int limit = 2;
            /*if (TLDList.getInstance().contains(domain)) {
                domain = parts[parts.length - 3] + "." + domain;
                limit = 3;
            }*/
            for (int i = 0; i < (parts.length - limit); i++) {
                if (!subDomain.isEmpty()) {
                    subDomain += ".";
                }
                subDomain += parts[i];
            }
        }
        path = url.substring(domainEndIdx);
        int pathEndIdx = path.indexOf('?');
        if (pathEndIdx >= 0) {
            path = path.substring(0, pathEndIdx);
        }
    }

    /*
     * @return
     *      url of the parent page. The parent page is the page in which
     *      the Url of this page is first observed.
     */


    public String getParentAnchor(){
        return parentAnchor;
    }

    public void setParentAnchor(String parentAnchor){
        this.parentAnchor = parentAnchor;
    }

    /**
     * @return
     *      crawl depth at which this Url is first observed. Seed Urls
     *      are at depth 0. Urls that are extracted from seed Urls are at depth 1, etc.
     */
    public short getDepth() {
        return depth;
    }

    public void setDepth(short depth) {
        this.depth = depth;
    }

    /**
     * @return
     *      domain of this Url. For 'http://www.example.com/sample.htm', domain will be 'example
     *      .com'
     */
    public String getDomain() {
        return domain;
    }

    public String getSubDomain() {
        return subDomain;
    }

    /**
     * @return
     *      path of this Url. For 'http://www.example.com/sample.htm', domain will be 'sample.htm'
     */
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return
     *      anchor string. For example, in <a href="example.com">A sample anchor</a>
     *      the anchor string is 'A sample anchor'
     */
    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    /**
     * @return priority for crawling this URL. A lower number results in higher priority.
     */
    public byte getPriority() {
        return priority;
    }

    public void setPriority(byte priority) {
        this.priority = priority;
    }

    /**
     * @return tag in which this URL is found
     * */
    /*public String getTag() {
        return tag;
    }*/

   /* public void setTag(String tag) {
        this.tag = tag;
    }*/

    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        WebURL otherUrl = (WebURL) o;
        return (url != null) && url.equals(otherUrl.getURL());

    }

    @Override
    public String toString() {
        return this.anchor+": "+this.depth+" "+"\n";
    }

}
