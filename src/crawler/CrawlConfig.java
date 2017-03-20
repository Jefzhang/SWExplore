package crawler;

/**
 * Created by jfzhang on 02/03/2017.
 */

//import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class CrawlConfig {

    /**
     * The folder which will be used by crawler for storing the intermediate
     * crawl data. The content of this folder should not be modified manually.
     */
    private String crawlStorageFolder;

    /**
     * The data structure which will be used for storing the url
     * 1 : concurrency database (simply synchronised operation)
     * 2 : lockfree queues
     */
    private int urlStorageType;


    /**
     * The data structure which will be used for storing all the visited urls
     * 1 : simply synchronised operations with a hashmap
     */
    private int urlServerType;


    /**
     * Maximum depth of crawling For unlimited depth this parameter should be
     * set to -1
     */
    private int maxDepthOfCrawling = 10;

    /**
     * Maximum number of pages to fetch For unlimited number of pages, this
     * parameter should be set to -1
     */
    private int maxPagesToFetch = -1;

    /**
     * user-agent string that is used for representing your crawler to web
     * servers. See http://en.wikipedia.org/wiki/User_agent for more details
     */
    private String userAgentString = "swexplore (https://github.com/Jefzhang/SWExplore/)";

    /**
     * Default request header values.
     */
    private Collection<BasicHeader> defaultHeaders = new HashSet<BasicHeader>();

    /**
     * Politeness delay in milliseconds (delay between sending two requests to
     * the same host).
     */
    private int politenessDelay = 200;

    /**
     * Should we also crawl https pages?
     */
    private boolean includeHttpsPages = true;

    /**
     * Should we fetch binary content such as images, audio, ...?
     */
    private boolean includeBinaryContentInCrawling = false;

    /**
     * Should we process binary content such as image, audio, ... using TIKA?
     */
    private boolean processBinaryContentInCrawling = false;

    /**
     * Maximum Connections per host
     */
    private int maxConnectionsPerHost = 100;

    /**
     * Maximum total connections
     */
    private int maxTotalConnections = 100;

    /**
     * Socket timeout in milliseconds
     */
    private int socketTimeout = 20000;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;

    /**
     * Max number of outgoing links which are processed from a page
     */
    private int maxOutgoingLinksToFollow = 5000;

    /**
     * Max allowed size of a page. Pages larger than this size will not be
     * fetched.
     */
    private int maxDownloadSize = 1048576;

    /**
     * Should we follow redirects?
     */
    private boolean followRedirects = true;

    /**
     * Should the TLD list be updated automatically on each run? Alternatively,
     * it can be loaded from the embedded tld-names.zip file that was obtained from
     * https://publicsuffix.org/list/effective_tld_names.dat
     */
    private boolean onlineTldListUpdate = false;

    /**
     * Should the crawler stop running when the queue is empty?
     */
    private boolean shutdownOnEmptyQueue = true;

    /**
     * Wait this long before checking the status of the worker threads.
     */
    private int threadMonitoringDelaySeconds = 1;

    /**
     * Wait this long to verify the craweler threads are finished working.
     */
    private int threadShutdownDelaySeconds = 1;

    /**
     * Wait this long in seconds before launching cleanup.
     */
    private int cleanupDelaySeconds = 1;



    /**
     * Validates the configs specified by this instance.
     *
     * @throws Exception on Validation fail
     */
    public void validate() throws Exception {
        if (crawlStorageFolder == null) {
            throw new Exception("Crawl storage folder is not set in the CrawlConfig.");
        }
        if (politenessDelay < 0) {
            throw new Exception("Invalid value for politeness delay: " + politenessDelay);
        }
        if (maxDepthOfCrawling < -1) {
            throw new Exception(
                    "Maximum crawl depth should be either a positive number or -1 for unlimited depth" +
                            ".");
        }
        if (maxDepthOfCrawling > Short.MAX_VALUE) {
            throw new Exception("Maximum value for crawl depth is " + Short.MAX_VALUE);
        }
    }

    public String getCrawlStorageFolder() {
        return crawlStorageFolder;
    }

    /**
     * The folder which will be used by crawler for storing the intermediate
     * crawl data. The content of this folder should not be modified manually.
     *
     * @param crawlStorageFolder The folder for the crawler's storage
     */
    public void setCrawlStorageFolder(String crawlStorageFolder) {
        this.crawlStorageFolder = crawlStorageFolder;
    }



    public int getUrlStorageType() {
        return urlStorageType;
    }

    /**
     * Choose the data structure used for storing all the intermediate urls
     * @param type The type de data structure used for storing the urls, 1 or 2
     */
    public void setUrlStorageType(int type){
        this.urlStorageType = type;
    }

    public int getUrlServerType() {
        return urlServerType;
    }

    /**
     * Choose the data structure used for storing all the visited urls
     * @param type 1 stand for simple synchronised hashmap
     */
    public void setUrlServerType(int type) {
        this.urlServerType = type;
    }


    public int getMaxDepthOfCrawling() {
        return maxDepthOfCrawling;
    }

    /**
     * Maximum depth of crawling For unlimited depth this parameter should be set to -1
     *
     * @param maxDepthOfCrawling Depth of crawling (all links on current page = depth of 1)
     */
    public void setMaxDepthOfCrawling(int maxDepthOfCrawling) {
        this.maxDepthOfCrawling = maxDepthOfCrawling;
    }

    public int getMaxPagesToFetch() {
        return maxPagesToFetch;
    }

    /**
     * Maximum number of pages to fetch For unlimited number of pages, this parameter should be
     * set to -1
     *
     * @param maxPagesToFetch How many pages to fetch from all threads together ?
     */
    public void setMaxPagesToFetch(int maxPagesToFetch) {
        this.maxPagesToFetch = maxPagesToFetch;
    }

    /**
     *
     * @return userAgentString
     */
    public String getUserAgentString() {
        return userAgentString;
    }

    /**
     * user-agent string that is used for representing your crawler to web
     * servers. See http://en.wikipedia.org/wiki/User_agent for more details
     *
     * @param userAgentString Custom userAgent string to use as your crawler's identifier
     */
    public void setUserAgentString(String userAgentString) {
        this.userAgentString = userAgentString;
    }

    /**
     * Return a copy of the default header collection.
     */
    public Collection<BasicHeader> getDefaultHeaders() {
        return new HashSet<>(defaultHeaders);
    }

    /**
     * Set the default header collection (creating copies of the provided headers).
     */
    public void setDefaultHeaders(Collection<? extends Header> defaultHeaders) {
        Collection<BasicHeader> copiedHeaders = new HashSet<>();
        for (Header header : defaultHeaders) {
            copiedHeaders.add(new BasicHeader(header.getName(), header.getValue()));
        }
        this.defaultHeaders = copiedHeaders;
    }

    public int getPolitenessDelay() {
        return politenessDelay;
    }

    /**
     * Politeness delay in milliseconds (delay between sending two requests to
     * the same host).
     *
     * @param politenessDelay
     *            the delay in milliseconds.
     */
    public void setPolitenessDelay(int politenessDelay) {
        this.politenessDelay = politenessDelay;
    }

    public boolean isIncludeHttpsPages() {
        return includeHttpsPages;
    }

    /**
     * @param includeHttpsPages Should we crawl https pages?
     */
    public void setIncludeHttpsPages(boolean includeHttpsPages) {
        this.includeHttpsPages = includeHttpsPages;
    }

    public boolean isIncludeBinaryContentInCrawling() {
        return includeBinaryContentInCrawling;
    }

    /**
     *
     * @param includeBinaryContentInCrawling Should we fetch binary content such as images,
     * audio, ...?
     */
    public void setIncludeBinaryContentInCrawling(boolean includeBinaryContentInCrawling) {
        this.includeBinaryContentInCrawling = includeBinaryContentInCrawling;
    }

    public boolean isProcessBinaryContentInCrawling() {
        return processBinaryContentInCrawling;
    }

    /**
     * Should we process binary content such as images, audio, ... using TIKA?
     */
    public void setProcessBinaryContentInCrawling(boolean processBinaryContentInCrawling) {
        this.processBinaryContentInCrawling = processBinaryContentInCrawling;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    /**
     * @param maxConnectionsPerHost Maximum Connections per host
     */
    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    /**
     * @param maxTotalConnections Maximum total connections
     */
    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @param socketTimeout Socket timeout in milliseconds
     */
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout Connection timeout in milliseconds
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getMaxOutgoingLinksToFollow() {
        return maxOutgoingLinksToFollow;
    }

    /**
     * @param maxOutgoingLinksToFollow Max number of outgoing links which are processed from a page
     */
    public void setMaxOutgoingLinksToFollow(int maxOutgoingLinksToFollow) {
        this.maxOutgoingLinksToFollow = maxOutgoingLinksToFollow;
    }

    public int getMaxDownloadSize() {
        return maxDownloadSize;
    }

    /**
     * @param maxDownloadSize Max allowed size of a page. Pages larger than this size will not be
     * fetched.
     */
    public void setMaxDownloadSize(int maxDownloadSize) {
        this.maxDownloadSize = maxDownloadSize;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    /**
     * @param followRedirects Should we follow redirects?
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public boolean isShutdownOnEmptyQueue() {
        return shutdownOnEmptyQueue;
    }

    /**
     * Should the crawler stop running when the queue is empty?
     */
    public void setShutdownOnEmptyQueue(boolean shutdown) {
        shutdownOnEmptyQueue = shutdown;
    }

    public boolean isOnlineTldListUpdate() {
        return onlineTldListUpdate;
    }

    /**
     * Should the TLD list be updated automatically on each run? Alternatively,
     * it can be loaded from the embedded tld-names.txt resource file that was
     * obtained from https://publicsuffix.org/list/effective_tld_names.dat
     */
    public void setOnlineTldListUpdate(boolean online) {
        onlineTldListUpdate = online;
    }


    public int getThreadMonitoringDelaySeconds() {
        return threadMonitoringDelaySeconds;
    }

    public void setThreadMonitoringDelaySeconds(int delay) {
        this.threadMonitoringDelaySeconds = delay;
    }

    public int getThreadShutdownDelaySeconds() {
        return threadShutdownDelaySeconds;
    }

    public void setThreadShutdownDelaySeconds(int delay) {
        this.threadShutdownDelaySeconds = delay;
    }

    public int getCleanupDelaySeconds() {
        return cleanupDelaySeconds;
    }

    public void setCleanupDelaySeconds(int delay) {
        this.cleanupDelaySeconds = delay;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Crawl storage folder: " + getCrawlStorageFolder() + "\n");
        //sb.append("Resumable crawling: " + isResumableCrawling() + "\n");
        sb.append("Max depth of crawl: " + getMaxDepthOfCrawling() + "\n");
        sb.append("Max pages to fetch: " + getMaxPagesToFetch() + "\n");
        sb.append("User agent string: " + getUserAgentString() + "\n");
        sb.append("Include https pages: " + isIncludeHttpsPages() + "\n");
        sb.append("Include binary content: " + isIncludeBinaryContentInCrawling() + "\n");
        sb.append("Max connections per host: " + getMaxConnectionsPerHost() + "\n");
        sb.append("Max total connections: " + getMaxTotalConnections() + "\n");
        sb.append("Socket timeout: " + getSocketTimeout() + "\n");
        sb.append("Max total connections: " + getMaxTotalConnections() + "\n");
        sb.append("Max outgoing links to follow: " + getMaxOutgoingLinksToFollow() + "\n");
        sb.append("Max download size: " + getMaxDownloadSize() + "\n");
        sb.append("Should follow redirects?: " + isFollowRedirects() + "\n");
        sb.append("Thread monitoring delay: " + getThreadMonitoringDelaySeconds() + "\n");
        sb.append("Thread shutdown delay: " + getThreadShutdownDelaySeconds() + "\n");
        sb.append("Cleanup delay: " + getCleanupDelaySeconds() + "\n");
        return sb.toString();
    }
}
