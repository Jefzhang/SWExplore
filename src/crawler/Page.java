package crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;

import parser.ParseData;
import url.WebURL;
/**
 * Created by jfzhang on 03/03/2017.
 */
public class Page {
    /**
     * The URL of this page.
     */
    protected WebURL url;
    /**
     * Status of the page
     */
    protected int statusCode;

    /**
     * The content of this page in binary format.
     */
    protected byte[] contentData;

    /**
     * The ContentType of this page.
     * For example: "text/html; charset=UTF-8"
     */
    protected String contentType;

    /**
     * The encoding of the content.
     * For example: "gzip"
     */
    protected String contentEncoding;

    /**
     * The charset of the content.
     * For example: "UTF-8"
     */
    protected String contentCharset;

    /**
     * Language of the Content.
     */
    private String language;

    /**
     * Headers which were present in the response of the fetch request
     */
    protected Header[] fetchResponseHeaders;

    /**
     * The parsed data populated by parsers
     */
    protected ParseData parseData;

    /**
     * Whether the content was truncated because the received data exceeded the imposed maximum
     */
    protected boolean truncated = false;

    public Page(WebURL url) {
        this.url = url;
    }

    /**
     * Loads the content of this page from a fetched HttpEntity.
     *
     * @param entity HttpEntity
     * @param maxBytes The maximum number of bytes to read
     * @throws Exception when load fails
     */
    public void load(HttpEntity entity, int maxBytes) throws Exception {

        contentType = null;
        Header type = entity.getContentType();
        if (type != null) {
            contentType = type.getValue();
        }

        contentEncoding = null;
        Header encoding = entity.getContentEncoding();
        if (encoding != null) {
            contentEncoding = encoding.getValue();
        }

        Charset charset = ContentType.getOrDefault(entity).getCharset();
        if (charset != null) {
            contentCharset = charset.displayName();
        }

        contentData = toByteArray(entity, maxBytes);
    }

    /**
     * Read contents from an entity, with a specified maximum. This is a replacement of
     * EntityUtils.toByteArray because that function does not impose a maximum size.
     *
     * @param entity The entity from which to read
     * @param maxBytes The maximum number of bytes to read
     * @return A byte array containing maxBytes or fewer bytes read from the entity
     *
     * @throws IOException Thrown when reading fails for any reason
     */
    protected byte[] toByteArray(HttpEntity entity, int maxBytes) throws IOException {
        if (entity == null) {
            return new byte[0];
        }

        InputStream is = entity.getContent();
        int size = (int) entity.getContentLength();
        if (size <= 0 || size > maxBytes) {
            size = maxBytes;
        }

        int actualSize = 0;

        byte[] buf = new byte[size];
        while (actualSize < size) {
            int remain = size - actualSize;
            int readBytes = is.read(buf, actualSize, Math.min(remain, 1500));

            if (readBytes <= 0) {
                break;
            }

            actualSize += readBytes;
        }

        // Poll to see if there are more bytes to read. If there are,
        // the content has been truncated
        int ch = is.read();
        if (ch >= 0) {
            truncated = true;
        }

        // If the actual size matches the size of the buffer, do not copy it
        if (actualSize == buf.length) {
            return buf;
        }

        // Return the subset of the byte buffer that was used
        return Arrays.copyOfRange(buf, 0, actualSize);
    }

    public WebURL getWebURL() {
        return url;
    }


    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }


    public void setFetchResponseHeaders(Header[] headers) {
        fetchResponseHeaders = headers;
    }

    /**
     * @return parsed data generated for this page by parsers
     */
    public ParseData getParseData() {
        return parseData;
    }

    public void setParseData(ParseData parseData) {
        this.parseData = parseData;
    }

    /**
     * @return content of this page in binary format.
     */
    public byte[] getContentData() {
        return contentData;
    }

    public void setContentData(byte[] contentData) {
        this.contentData = contentData;
    }

    /**
     * @return charset of the content.
     * For example: "UTF-8"
     */
    public String getContentCharset() {
        return contentCharset;
    }

    public void setContentCharset(String contentCharset) {
        this.contentCharset = contentCharset;
    }


    public boolean isTruncated() {
        return truncated;
    }

    @Override
    public String toString(){
        return new String(this.contentData);
    }


}
