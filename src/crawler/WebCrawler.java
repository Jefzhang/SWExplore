package crawler;

/**
 * Created by jfzhang on 02/03/2017.
 */

import crawler.exception.ContentFetchException;
import crawler.exception.PageExceedSizeException;
import crawler.exception.ParseException;
import multithread.UrlDiscovered;
//import multithread.UrlIdServer;
import multithread.WebUrlQueues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import parser.Parser;
import parser.ParseData;

import fetcher.PageFetcher;
import fetcher.PageFetchResult;
import url.WebURL;

import java.util.List;
import java.util.ArrayList;


public class WebCrawler implements Runnable{

    protected static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

    /**
     * The id associated to the crawler thread running this instance
     */
    protected int myId;

    /**
     * The controller instance that has created this crawler thread. This
     * reference to the controller can be used for getting configurations of the
     * current crawl or adding new seeds during runtime.
     */
    protected SimpleController myController;

    /**
     * The thread within which this crawler instance is running.
     */
    private Thread myThread;

    /**
     * The Parser.Parser that is used by this crawler instance to parse the content of the fetched pages.
     */
    private Parser parser;

    /**
     * The fetcher that is used by this crawler instance to fetch the content of pages from the web.
     */
    private PageFetcher pageFetcher;


    /**
     * The urlIdServer that is used by this crawler instance to map each URL to a unique docid.
     */
    private UrlDiscovered urlBase;

    /**
     * The Frontier object that manages the crawl queue.
     */

    private WebUrlQueues queue;
    //private Frontier frontier;

    /**
     *
     */
    private List<WebURL> characterSet;


    /**
     * Is the current crawler instance waiting for new URLs? This field is
     * mainly used by the controller to detect whether all of the crawler
     * instances are waiting for new URLs and therefore there is no more work
     * and crawling can be stopped.
     */
    private boolean isWaitingForNewURLs;

    /**
     * The number of pages that the crawler processed
     */
    private int numPagesVisited;

    /**
     * Initializes the current instance of the crawler
     *
     * @param id
     *            the id of this crawler instance
     * @param crawlController
     *            the controller that manages this crawling session
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void init(int id, /*SimpleController*/SimpleController  crawlController)
            throws InstantiationException, IllegalAccessException {
        this.myId = id;
        this.pageFetcher = crawlController.getPageFetcher();
        //this.robotstxtServer = crawlController.getRobotstxtServer();
        this.urlBase = crawlController.getUrlBase();
        this.queue = crawlController.getFrontier();
        this.parser = new Parser(crawlController.getConfig());
        this.myController = crawlController;
        this.characterSet = new ArrayList<>();
        this.isWaitingForNewURLs = false;
        this.numPagesVisited = 0;
    }

    /**
     * Get the id of the current crawler instance
     *
     * @return the id of the current crawler instance
     */
    public int getMyId() {
        return myId;
    }

    public SimpleController getMyController() {
        return myController;
    }



    /**
     * This function is called if the content of a url is bigger than allowed size.
     *
     * @param urlStr - The URL which it's content is bigger than allowed size
     */
    protected void onPageBiggerThanMaxSize(String urlStr, long pageSize) {
        logger.warn("Skipping a URL: {} which was bigger ( {} ) than max allowed size", urlStr,
                pageSize);
    }
    /**
     * This function is called if the content of a url could not be fetched.
     *
     * @param webUrl URL which content failed to be fetched
     */
    protected void onContentFetchError(WebURL webUrl) {
        logger.warn("Can't fetch content of: {}", webUrl.getURL());
    }

    /**
     * This function is called when a unhandled exception was encountered during fetching
     *
     * @param webUrl URL where a unhandled exception occured
     */
    protected void onUnhandledException(WebURL webUrl, Throwable e) {
        String urlStr = (webUrl == null ? "NULL" : webUrl.getURL());
        logger.warn("Unhandled exception while fetching {}: {}", urlStr, e.getMessage());
        logger.info("Stacktrace: ", e);
    }

    /**
     * This function is called if there has been an error in parsing the content.
     *
     * @param webUrl URL which failed on parsing
     */
    protected void onParseError(WebURL webUrl) {
        logger.warn("Parsing error of: {}", webUrl.getURL());
    }

    /**
     * The CrawlController instance that has created this crawler instance will
     * call this function just before terminating this crawler thread.
     * The controller then puts these local data in a list
     * and then store it on disk
     * @return List
     */
    public List<WebURL> getMyLocalData() {
        return this.characterSet;
    }

    public int getNumPagesVisited(){
        return this.numPagesVisited;
    }

    @Override
    public void run() {
        while (true) {
            List<WebURL> assignedURLs = new ArrayList<>(10);           //get next 10 urls
            isWaitingForNewURLs = true;
            queue.getNextURLs(5, assignedURLs);
            if (assignedURLs.isEmpty()) {
                if (queue.isFinished()) {                         //shut down this crawler when there is no more url
                    return;
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    logger.error("Error occurred", e);
                }
            } else {
                isWaitingForNewURLs = false;
                for (WebURL curURL : assignedURLs) {
                    if (myController.isShuttingDown()) {
                        queue.schedule(curURL);
                        logger.info("Exiting because of controller shutdown.");
                        return;
                    }
                    if (curURL != null) {
                        numPagesVisited++;
                        processPage(curURL);
                    }
                }
            }
        }
    }

    /**
     * Classes that extends WebCrawler should overwrite this function to tell the
     * crawler whether the given url should be crawled or not. The following
     * default implementation indicates that all urls should be included in the crawl.
     *
     * @param url
     *            the url which we are interested to know whether it should be
     *            included in the crawl or not.
     * @param referringPage
     *           The Page in which this url was found.
     * @return if the url should be included in the crawl it returns true,
     *         otherwise false is returned.
     */
    public boolean shouldVisit(Page referringPage, WebURL url) {
        // By default allow all urls to be crawled.
        String href = url.getURL().toLowerCase();
        return href.startsWith("http://starwars.wikia.com/wiki/");
    }

    /**
     *
     * Determine whether links found at the given URL should be added to the queue for crawling.
     * In our application, we should follow the links if this is a character
     * @param page the page under consideration
     * @return true if outgoing links from this page should be added to the queue.
     */
    protected boolean shouldFollowLinksIn(Page page) {
        ParseData parseData= page.getParseData();
        return parseData.isCharacter();
    }

    private void processPage(WebURL curURL){
        PageFetchResult fetchResult = null;
        try {
            if (curURL == null) {
                return;
            }

            fetchResult = pageFetcher.fetchPage(curURL);
            int statusCode = fetchResult.getStatusCode();
            Page page = new Page(curURL);
            page.setFetchResponseHeaders(fetchResult.getResponseHeaders());
            page.setStatusCode(statusCode);
                if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {    //redirect page
                    if (urlBase.contains(fetchResult.getFetchedUrl())) {
                        logger.debug("Redirect page: {} has already been seen", curURL);
                        return;
                    }
                    if(!(fetchResult.getFetchedUrl()==null)) {
                        curURL.setURL(fetchResult.getFetchedUrl());                   //update the url
                    }
                }

                if (!fetchResult.fetchContent(page,
                        myController.getConfig().getMaxDownloadSize())) {
                    throw new ContentFetchException(curURL.getURL());
                }

                if (page.isTruncated()) {
                    logger.warn(
                            "Warning: unknown page size exceeded max-download-size, truncated to: " +
                                    "({}), at URL: {}",
                            myController.getConfig().getMaxDownloadSize(), curURL.getURL());
                }
                parser.parse(page, curURL.getURL());

                if (shouldFollowLinksIn(page)) {

                    this.characterSet.add(curURL);        //store
                    ParseData parseData = page.getParseData();
                    List<WebURL> toSchedule = new ArrayList<>();
                    int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
                    for (WebURL webURL : parseData.getOutgoingUrls()) {
                        webURL.setParentAnchor(curURL.getAnchor());
                        webURL.setDepth((short) (curURL.getDepth() + 1));
                            if ((maxCrawlDepth == -1) || (curURL.getDepth() < maxCrawlDepth)) {
                                if (shouldVisit(page, webURL)&&!webURL.getURL().startsWith(curURL.getURL())) {
                                    if(urlBase.add(webURL.getURL()))
                                        toSchedule.add(webURL);
                                } else {
                                    logger.debug(
                                            "Not visiting: {} as per your \"shouldVisit\" policy",
                                            webURL.getURL());
                                }
                            }
                    }
                    queue.scheduleAll(toSchedule);
                } else {
                    logger.debug("Not looking for links in page {}, "
                                    + "as per your \"shouldFollowLinksInPage\" policy",
                            page.getWebURL().getURL());
                }
        } catch (PageExceedSizeException e) {
            onPageBiggerThanMaxSize(curURL.getURL(), e.getPageSize());
        } catch (ParseException pe) {
            onParseError(curURL);
        } catch (ContentFetchException cfe) {
            onContentFetchError(curURL);
        } catch (Exception e) {
            onUnhandledException(curURL, e);
        } finally {
            if (fetchResult != null) {
                fetchResult.discardContentIfNotConsumed();
            }
        }
    }

    public Thread getThread() {
        return myThread;
    }

    public void setThread(Thread myThread) {
        this.myThread = myThread;
    }

    public boolean isNotWaitingForNewURLs() {
        return !isWaitingForNewURLs;
    }


}


