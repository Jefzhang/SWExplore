package crawler;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import fetcher.PageFetcher;
import multithread.Frontier;
import multithread.UrlIdServer;
import multithread.WebUrlQueues;
import url.URLnormlization;
import url.WebURL;
import util.IO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jfzhang on 05/03/2017.
 */
public class SimpleController extends Configurable{


    static final Logger logger = LoggerFactory.getLogger(SimpleController.class);
    protected boolean finished;

    protected ArrayList<WebURL> crawlersLocalData = new ArrayList<>();

    /**
     * Is the crawling session set to 'shutdown'. Crawler threads monitor this
     * flag and when it is set they will no longer process new pages.
     */
    protected boolean shuttingDown;

    protected PageFetcher pageFetcher;
    //protected RobotstxtServer robotstxtServer;
    protected WebUrlQueues frontier;
    protected UrlIdServer docIdServer;

    public SimpleController(CrawlConfig config, PageFetcher pageFetcher) throws Exception {

        super(config);
        config.validate();

        /*
        File folder = new File(config.getCrawlStorageFolder());
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                logger.debug("Created folder: " + folder.getAbsolutePath());
            } else {
                throw new Exception(
                        "couldn't create the storage folder: " + folder.getAbsolutePath() +
                                " does it already exist ?");
            }
        }


        File envHome = new File(config.getCrawlStorageFolder() + "/frontier");
        if (!envHome.exists()) {
            if (envHome.mkdir()) {
                logger.debug("Created folder: " + envHome.getAbsolutePath());
            } else {
                throw new Exception(
                        "Failed creating the frontier folder: " + envHome.getAbsolutePath());
            }
        }

        IO.deleteFolderContents(envHome);
        logger.info("Deleted contents of: " + envHome +
                " ( as you have configured resumable crawling to false )");


        env = new Environment(envHome, envConfig);
        */
        docIdServer = new UrlIdServer();
        frontier = new WebUrlQueues();

        this.pageFetcher = pageFetcher;
        //this.robotstxtServer = robotstxtServer;

        finished = false;
        shuttingDown = false;
    }

    public interface WebCrawlerFactory<T extends WebCrawler> {
        T newInstance() throws Exception;
    }

    private static class DefaultWebCrawlerFactory<T extends WebCrawler>
            implements WebCrawlerFactory<T> {
        final Class<T> clazz;

        DefaultWebCrawlerFactory(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T newInstance() throws Exception {
            try {
                return clazz.newInstance();
            } catch (ReflectiveOperationException e) {
                throw e;
            }
        }
    }

    public <T extends WebCrawler> void start(Class<T> clazz, int numberOfCrawlers) {
        this.start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers);
    }


    protected <T extends WebCrawler> void start(final WebCrawlerFactory<T> crawlerFactory,
                                                final int numberOfCrawlers) {
        try {

            long before = System.nanoTime();
            finished = false;
            crawlersLocalData.clear();
            final List<Thread> threads = new ArrayList<>();
            final List<T> crawlers = new ArrayList<>();

            for (int i = 1; i <= numberOfCrawlers; i++) {
                T crawler = crawlerFactory.newInstance();
                Thread thread = new Thread(crawler, "Crawler " + i);
                crawler.setThread(thread);
                crawler.init(i, this);
                thread.start();
                crawlers.add(crawler);
                threads.add(thread);
                logger.info("Crawler {} started", i);
            }

            final SimpleController controller = this;
            final CrawlConfig config = this.getConfig();

            Thread monitorThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (true) {
                                sleep(config.getThreadMonitoringDelaySeconds());
                                boolean someoneIsWorking = false;
                                for (int i = 0; i < threads.size(); i++) {
                                    Thread thread = threads.get(i);
                                    if (!thread.isAlive()) {
                                        if (!shuttingDown) {
                                            logger.info("Thread {} was dead, I'll recreate it", i);
                                            T crawler = crawlerFactory.newInstance();
                                            thread = new Thread(crawler, "Crawler " + (i + 1));
                                            threads.remove(i);
                                            threads.add(i, thread);
                                            crawler.setThread(thread);
                                            crawler.init(i + 1, controller);
                                            thread.start();
                                            crawlers.remove(i);
                                            crawlers.add(i, crawler);
                                        }
                                    } else if (crawlers.get(i).isNotWaitingForNewURLs()) {
                                        someoneIsWorking = true;
                                    }
                                }
                                boolean shutOnEmpty = config.isShutdownOnEmptyQueue();
                                if (!someoneIsWorking && shutOnEmpty) {
                                    // Make sure again that none of the threads
                                    // are
                                    // alive.
                                    logger.info(
                                            "It looks like no thread is working, waiting for " +
                                                    config.getThreadShutdownDelaySeconds() +
                                                    " seconds to make sure...");
                                    sleep(config.getThreadShutdownDelaySeconds());

                                    someoneIsWorking = false;
                                    for (int i = 0; i < threads.size(); i++) {
                                        Thread thread = threads.get(i);
                                        if (thread.isAlive() &&
                                                crawlers.get(i).isNotWaitingForNewURLs()) {
                                            someoneIsWorking = true;
                                        }
                                    }
                                    if (!someoneIsWorking) {
                                        if (!shuttingDown) {
                                            //long queueLength = frontier.getQueueLength();
                                            if (!frontier.isEmpty()) {
                                                continue;
                                            }
                                            logger.info(
                                                    "No thread is working and no more URLs are in " +
                                                            "queue waiting for another " +
                                                            config.getThreadShutdownDelaySeconds() +
                                                            " seconds to make sure...");
                                            sleep(config.getThreadShutdownDelaySeconds());
                                            //queueLength = frontier.getQueueLength();
                                            if (!frontier.isEmpty()) {
                                                continue;
                                            }
                                        }

                                        logger.info(
                                                "All of the crawlers are stopped. Finishing the " +
                                                        "process...");
                                        // At this step, frontier notifies the threads that were
                                        // waiting for new URLs and they should stop
                                        frontier.finish();
                                        for (T crawler : crawlers) {
                                            crawler.onBeforeExit();
                                            crawlersLocalData.addAll(crawler.getMyLocalData());
                                        }
                                        FileWriter file = new FileWriter(controller.getConfig().getCrawlStorageFolder(),true);
                                        BufferedWriter write = new BufferedWriter(file);
                                        for (WebURL webURL : crawlersLocalData){
                                            write.write(webURL.toString());
                                        }
                                        write.close();
                                        file.close();

                                        logger.info(
                                                "Waiting for " + config.getCleanupDelaySeconds() +
                                                        " seconds before final clean up...");
                                        sleep(config.getCleanupDelaySeconds());

                                        pageFetcher.shutDown();

                                        finished = true;

                                        return;
                                    }
                                }
                            }
                    } catch (Exception e) {
                        logger.error("Unexpected Error", e);
                    }
                }
            });

            monitorThread.start();
            monitorThread.join();

            /*if (isBlocking) {
                waitUntilFinish();
            }*/
            long after = System.nanoTime();
            double complexity = (after - before) / 1_000_000_000.0;
            System.out.format("Finished in %fs.\n", complexity);

        } catch (Exception e) {
            logger.error("Error happened", e);
        }
    }

    protected static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ignored) {
            // Do nothing
        }
    }

    /*public void waitUntilFinish() {
        while (!finished) {
            synchronized (waitingLock) {
                if (finished) {
                    return;
                }
                try {
                    waitingLock.wait();
                } catch (InterruptedException e) {
                    logger.error("Error occurred", e);
                }
            }
        }
    }*/

    /**
     * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
     * to extract new URLs in it and follow them for crawling.
     *
     * @param pageUrl
     *            the URL of the seed
     */
    public void addSeed(String pageUrl) {
        addSeed(pageUrl, -1);
    }

    /**
     * Adds a new seed URL. A seed URL is a URL that is fetched by the crawler
     * to extract new URLs in it and follow them for crawling. You can also
     * specify a specific document id to be assigned to this seed URL. This
     * document id needs to be unique. Also, note that if you add three seeds
     * with document ids 1,2, and 7. Then the next URL that is found during the
     * crawl will get a doc id of 8. Also you need to ensure to add seeds in
     * increasing order of document ids.
     *
     * Specifying doc ids is mainly useful when you have had a previous crawl
     * and have stored the results and want to start a new crawl with seeds
     * which get the same document ids as the previous crawl.
     *
     * @param pageUrl
     *            the URL of the seed
     * @param docId
     *            the document id that you want to be assigned to this seed URL.
     *
     */
    public void addSeed(String pageUrl, int docId) {
        String canonicalUrl = URLnormlization.getCanonicalURL(pageUrl);
        if (canonicalUrl == null) {
            logger.error("Invalid seed URL: {}", pageUrl);
        } else {
            if (docId < 0) {
                docId = docIdServer.getUrlId(canonicalUrl);
                if (docId > 0) {
                    logger.trace("This URL is already seen.");
                    return;
                }
                docId = docIdServer.getNewUrlID(canonicalUrl);
            } else {
                try {
                    docIdServer.addUrlIdPair(canonicalUrl, docId);
                } catch (Exception e) {
                    logger.error("Could not add seed: {}", e.getMessage());
                }
            }

            WebURL webUrl = new WebURL();
            webUrl.setURL(canonicalUrl);
            webUrl.setDocid(docId);
            webUrl.setDepth((short) 0);
            //if (robotstxtServer.allows(webUrl)) {
            frontier.schedule(webUrl);
            /*} else {
                // using the WARN level here, as the user specifically asked to add this seed
                logger.warn("Robots.txt does not allow this seed: {}", pageUrl);
            }*/
        }
    }

    /**
     * This function can called to assign a specific document id to a url. This
     * feature is useful when you have had a previous crawl and have stored the
     * Urls and their associated document ids and want to have a new crawl which
     * is aware of the previously seen Urls and won't re-crawl them.
     *
     * Note that if you add three seen Urls with document ids 1,2, and 7. Then
     * the next URL that is found during the crawl will get a doc id of 8. Also
     * you need to ensure to add seen Urls in increasing order of document ids.
     *
     * @param url
     *            the URL of the page
     * @param docId
     *            the document id that you want to be assigned to this URL.
     *
     */
    public void addSeenUrl(String url, int docId) {
        String canonicalUrl = URLnormlization.getCanonicalURL(url);
        if (canonicalUrl == null) {
            logger.error("Invalid Url: {} (can't cannonicalize it!)", url);
        } else {
            try {
                docIdServer.addUrlIdPair(canonicalUrl, docId);
            } catch (Exception e) {
                logger.error("Could not add seen url: {}", e.getMessage());
            }
        }
    }

    public PageFetcher getPageFetcher() {
        return pageFetcher;
    }

    public void setPageFetcher(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }


    public WebUrlQueues getFrontier() {
        return frontier;
    }

    public void setFrontier(WebUrlQueues frontier) {
        this.frontier = frontier;
    }

    public UrlIdServer getDocIdServer() {
        return docIdServer;
    }

    public void setDocIdServer(UrlIdServer docIdServer) {
        this.docIdServer = docIdServer;
    }

    /*public Object getCustomData() {
        return customData;
    }*/

    /*public void setCustomData(Object customData) {
        this.customData = customData;
    }*/

    public boolean isFinished() {
        return this.finished;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * Set the current crawling session set to 'shutdown'. Crawler threads
     * monitor the shutdown flag and when it is set to true, they will no longer
     * process new pages.
     */
    public void shutdown() {
        logger.info("Shutting down...");
        this.shuttingDown = true;
        pageFetcher.shutDown();
        frontier.finish();
    }

    public static void main(String []args)throws Exception{
        String folder = "./data/output.txt";
        int numofCrawlers = Runtime.getRuntime().availableProcessors();
        int maxDepth = 2;



        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(folder);
        config.setMaxDepthOfCrawling(maxDepth);

        PageFetcher pageFetcher = new PageFetcher(config);
        SimpleController controller = new SimpleController(config, pageFetcher);

        controller.addSeed("http://starwars.wikia.com/wiki/Yoda");
        //controller.addSeed("http://www.ics.uci.edu/~welling/");
        //controller.addSeed("http://www.ics.uci.edu/");

        /*
         * Start the crawl. This is a blocking operation, meaning that your code
         * will reach the line after this only when crawling is finished.
         */
        controller.start(WebCrawler.class, numofCrawlers);



    }











}
