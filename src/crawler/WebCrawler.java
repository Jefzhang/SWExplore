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

import parser.HtmlParseData;
import parser.Parser;
import parser.ParseData;

import fetcher.PageFetcher;
import fetcher.PageFetchResult;
import url.WebURL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;



import multithread.Frontier;





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
    //protected Controller myController;

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
     * This function is called just before starting the crawl by this crawler
     * instance. It can be used for setting up the data structures or
     * initializations needed by this crawler instance.
     */
    public void onStart() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called just before the termination of the current
     * crawler instance. It can be used for persisting in-memory data or other
     * finalization tasks.
     */
    public void onBeforeExit() {
        // Do nothing by default
        // Sub-classed can override this to add their custom functionality
    }


    /**
     * This function is called before processing of the page's URL
     * It can be overridden by subclasses for tweaking of the url before processing it.
     * For example, http://abc.com/def?a=123 - http://abc.com/def
     *
     * @param curURL current URL which can be tweaked before processing
     * @return tweaked WebURL
     */
    protected WebURL handleUrlBeforeProcess(WebURL curURL) {
        return curURL;
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
     * This function is called if the crawler encounters a page with a 3xx status code
     *
     * @param page Partial page object
     */
    protected void onRedirectedStatusCode(Page page) {
        //Subclasses can override this to add their custom functionality
    }


    /**
     * This function is called if the content of a url could not be fetched.
     *
     * @param webUrl URL which content failed to be fetched
     */
    protected void onContentFetchError(WebURL webUrl) {
        logger.warn("Can't fetch content of: {}", webUrl.getURL());
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
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
        // Do nothing by default (except basic logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * This function is called if there has been an error in parsing the content.
     *
     * @param webUrl URL which failed on parsing
     */
    protected void onParseError(WebURL webUrl) {
        logger.warn("Parsing error of: {}", webUrl.getURL());
        // Do nothing by default (Except logging)
        // Sub-classed can override this to add their custom functionality
    }

    /**
     * The CrawlController instance that has created this crawler instance will
     * call this function just before terminating this crawler thread. Classes
     * that extend WebCrawler can override this function to pass their local
     * data to their controller. The controller then puts these local data in a
     * List that can then be used for processing the local data of crawlers (if needed).
     *
     * @return currently NULL
     */
    public List<WebURL> getMyLocalData() {
        return this.characterSet;
    }

    @Override
    public void run() {
        onStart();
        while (true) {
            List<WebURL> assignedURLs = new ArrayList<>(10);           //get next 10 urls
            isWaitingForNewURLs = true;
            queue.getNextURLs(10, assignedURLs);
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
                        logger.info("Exiting because of controller shutdown.");
                        return;
                    }
                    if (curURL != null) {
                        curURL = handleUrlBeforeProcess(curURL);
                        processPage(curURL);
                       // frontier.setProcessed(curURL);
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
     * By default this method returns true always, but classes that extend WebCrawler can
     * override it in order to implement particular policies about which pages should be
     * mined for outgoing links and which should not.
     *
     * If links from the URL are not being followed, then we are not operating as
     * a web crawler and need not check robots.txt before fetching the single URL.
     * (see definition at http://www.robotstxt.org/faq/what.html).  Thus URLs that
     * return false from this method will not be subject to robots.txt filtering.
     *
     * @param page the page under consideration
     * @return true if outgoing links from this page should be added to the queue.
     */
    protected boolean shouldFollowLinksIn(Page page) {
        ParseData parseData= page.getParseData();
        return parseData.isCharacter();
        //return true;
    }

    /**
     * Classes that extends WebCrawler should overwrite this function to process
     * the content of the fetched and parsed page.
     *
     * @param page
     *            the page object that is just fetched and parsed.
     */
    public void visit(Page page) {
        // Do nothing by default
        // Sub-classed should override this to add their custom functionality
        /*WebURL weburl = page.getWebURL();
        System.out.println(weburl.getURL());
        System.out.println(weburl.getAnchor()+" "+weburl.getDepth());*/
    }

    private void processPage(WebURL curURL){
        PageFetchResult fetchResult = null;
        try {
            if (curURL == null) {
                return;
            }

            fetchResult = pageFetcher.fetchPage(curURL);
            int statusCode = fetchResult.getStatusCode();
            /*handlePageStatusCode(curURL, statusCode,
                    EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode,
                            Locale.ENGLISH));*/
            // Finds the status reason for all known statuses

            Page page = new Page(curURL);
            //System.out.println(fetchResult.getResponseHeaders().toString());
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
                    //curURL.setDocid(urlIdServer.getNewUrlID(fetchResult.getFetchedUrl()));
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
                //System.out.println(page.toString());


                parser.parse(page, curURL.getURL());

                if (shouldFollowLinksIn(page)) {
                    System.out.println(page.getWebURL().getURL());
                    System.out.println(page.getWebURL().getAnchor());

                    this.characterSet.add(curURL);        //store
                    ParseData parseData = page.getParseData();
                    List<WebURL> toSchedule = new ArrayList<>();
                    int maxCrawlDepth = myController.getConfig().getMaxDepthOfCrawling();
                    for (WebURL webURL : parseData.getOutgoingUrls()) {
                        //webURL.setParentDocid(curURL.getDocid());
                        webURL.setParentUrl(curURL.getURL());
                        webURL.setParentAnchor(curURL.getAnchor());
                        webURL.setDepth((short) (curURL.getDepth() + 1));
                        //if (urlBase.contains(webURL.getURL()) || ) {

                        //} else {
                            //webURL.setDocid(-1);

                            if ((maxCrawlDepth == -1) || (curURL.getDepth() < maxCrawlDepth)) {
                                if (shouldVisit(page, webURL)&&!webURL.getURL().startsWith(curURL.getURL())) {
                                    //if (robotstxtServer.allows(webURL)) {
                                        //webURL.setDocid(urlIdServer.getNewUrlID(webURL.getURL()));
                                    if(urlBase.add(webURL.getURL()))
                                        toSchedule.add(webURL);
                                } else {
                                    logger.debug(
                                            "Not visiting: {} as per your \"shouldVisit\" policy",
                                            webURL.getURL());
                                }
                            }
                       // }
                    }
                    queue.scheduleAll(toSchedule);
                } else {
                    logger.debug("Not looking for links in page {}, "
                                    + "as per your \"shouldFollowLinksInPage\" policy",
                            page.getWebURL().getURL());
                }
                visit(page);
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


