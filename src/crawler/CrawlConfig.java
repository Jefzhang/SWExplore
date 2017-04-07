package crawler;

/**
 * Created by jfzhang on 02/03/2017.
 */

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class CrawlConfig {

    /**
     * If this is set to true, crawler
     */
    private boolean resumable = false;

    /**
     * The folder which will be used by crawler for storing the intermediate
     * crawl data. The content of this folder should not be modified manually.
     */
    private String crawlStorageFolder;


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
    private int politenessDelay = 100;

    /**
     * Should we also crawl https pages?
     */
    private boolean includeHttpsPages = true;

    /**
     * Should we fetch binary content such as images, audio, ...?
     */
    private boolean includeBinaryContentInCrawling = false;


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

    public boolean getResumable(){
        return this.resumable;
    }

    public void setResumable(boolean isResumable){
        this.resumable = isResumable;
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
     * Return a copy of the default header collection.
     */
    public Collection<BasicHeader> getDefaultHeaders() {
        return new HashSet<>(defaultHeaders);
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



    public boolean isIncludeBinaryContentInCrawling() {
        return includeBinaryContentInCrawling;
    }



    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }


    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }


    public int getSocketTimeout() {
        return socketTimeout;
    }


    public int getConnectionTimeout() {
        return connectionTimeout;
    }


    public int getMaxOutgoingLinksToFollow() {
        return maxOutgoingLinksToFollow;
    }


    public int getMaxDownloadSize() {
        return maxDownloadSize;
    }


    public boolean isFollowRedirects() {
        return followRedirects;
    }


    public boolean isShutdownOnEmptyQueue() {
        return shutdownOnEmptyQueue;
    }


    public int getThreadMonitoringDelaySeconds() {
        return threadMonitoringDelaySeconds;
    }


    public int getThreadShutdownDelaySeconds() {
        return threadShutdownDelaySeconds;
    }

    public int getCleanupDelaySeconds() {
        return cleanupDelaySeconds;
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
