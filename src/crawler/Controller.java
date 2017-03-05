package crawler;

import multithread.UrlIdServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import url.URLnormlization;
import util.IO;


import fetcher.PageFetcher;
import multithread.Frontier;
import url.WebURL;

/**
 * Created by jfzhang on 03/03/2017.
 */
public class Controller extends Configurable{
    static final Logger logger = LoggerFactory.getLogger(Controller.class);

    /**
     * The 'customData' object can be used for passing custom crawl-related
     * configurations to different components of the crawler.
     */
    protected Object customData;

    /**
     * Once the crawling session finishes the controller collects the local data
     * of the crawler threads and stores them in this List.
     */
    protected List<Object> crawlersLocalData = new ArrayList<>();

    /**
     * Is the crawling of this session finished?
     */
    protected boolean finished;

    /**
     * Is the crawling session set to 'shutdown'. Crawler threads monitor this
     * flag and when it is set they will no longer process new pages.
     */
    protected boolean shuttingDown;

    protected PageFetcher pageFetcher;
    //protected RobotstxtServer robotstxtServer;
    protected Frontier frontier;
    protected UrlIdServer docIdServer;


    protected final Object waitingLock = new Object();
    protected final Environment env;

    public Controller(CrawlConfig config, PageFetcher pageFetcher) throws Exception {
        super(config);

        config.validate();
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

        //TLDList.setUseOnline(config.isOnlineTldListUpdate());

        //boolean resumable = config.isResumableCrawling();

        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        //envConfig.setTransactional(resumable);
        //envConfig.setLocking(resumable);

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
        docIdServer = new UrlIdServer();
        frontier = new Frontier(env, config);

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

    /**
     * Start the crawling session and wait for it to finish.
     * This method utilizes default crawler factory that creates new crawler using Java reflection
     *
     * @param clazz
     *            the class that implements the logic for crawler threads
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(Class<T> clazz, int numberOfCrawlers) {
        this.start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers, true);
    }

    /**
     * Start the crawling session and wait for it to finish.
     *
     * @param crawlerFactory
     *            factory to create crawlers on demand for each thread
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void start(WebCrawlerFactory<T> crawlerFactory,
                                             int numberOfCrawlers) {
        this.start(crawlerFactory, numberOfCrawlers, true);
    }

    /**
     * Start the crawling session and return immediately.
     *
     * @param crawlerFactory
     *            factory to create crawlers on demand for each thread
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void startNonBlocking(WebCrawlerFactory<T> crawlerFactory,
                                                        final int numberOfCrawlers) {
        this.start(crawlerFactory, numberOfCrawlers, false);
    }

    /**
     * Start the crawling session and return immediately.
     * This method utilizes default crawler factory that creates new crawler using Java reflection
     *
     * @param clazz
     *            the class that implements the logic for crawler threads
     * @param numberOfCrawlers
     *            the number of concurrent threads that will be contributing in
     *            this crawling session.
     * @param <T> Your class extending WebCrawler
     */
    public <T extends WebCrawler> void startNonBlocking(Class<T> clazz, int numberOfCrawlers) {
        start(new DefaultWebCrawlerFactory<>(clazz), numberOfCrawlers, false);
    }

    protected <T extends WebCrawler> void start(final WebCrawlerFactory<T> crawlerFactory,
                                                final int numberOfCrawlers, boolean isBlocking) {
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
                //crawler.init(i, this);
                thread.start();
                crawlers.add(crawler);
                threads.add(thread);
                logger.info("Crawler {} started", i);
            }

            final Controller controller = this;
            final CrawlConfig config = this.getConfig();

            Thread monitorThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        synchronized (waitingLock) {
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
                                            //crawler.init(i + 1, controller);
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
                                            long queueLength = frontier.getQueueLength();
                                            if (queueLength > 0) {
                                                continue;
                                            }
                                            logger.info(
                                                    "No thread is working and no more URLs are in " +
                                                            "queue waiting for another " +
                                                            config.getThreadShutdownDelaySeconds() +
                                                            " seconds to make sure...");
                                            sleep(config.getThreadShutdownDelaySeconds());
                                            queueLength = frontier.getQueueLength();
                                            if (queueLength > 0) {
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
                                            crawlersLocalData.add(crawler.getMyLocalData());
                                        }

                                        logger.info(
                                                "Waiting for " + config.getCleanupDelaySeconds() +
                                                        " seconds before final clean up...");
                                        sleep(config.getCleanupDelaySeconds());

                                        frontier.close();
                                        //docIdServer.close();
                                        pageFetcher.shutDown();

                                        finished = true;
                                        waitingLock.notifyAll();
                                        env.close();

                                        return;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Unexpected Error", e);
                    }
                }
            });

            monitorThread.start();

            if (isBlocking) {
                waitUntilFinish();
            }
            long after = System.nanoTime();
            double complexity = (after - before) / 1_000_000_000.0;
            System.out.format("Finished in %fs.\n", complexity);

        } catch (Exception e) {
            logger.error("Error happened", e);
        }
    }

    /**
     * Wait until this crawling session finishes.
     */
    public void waitUntilFinish() {
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
    }

    /**
     * Once the crawling session finishes the controller collects the local data of the crawler
     * threads and stores them
     * in a List.
     * This function returns the reference to this list.
     *
     * @return List of Objects which are your local data
     */
    public List<Object> getCrawlersLocalData() {
        return crawlersLocalData;
    }

    protected static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ignored) {
            // Do nothing
        }
    }

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

    /*public RobotstxtServer getRobotstxtServer() {
        return robotstxtServer;
    }*/

    /*public void setRobotstxtServer(RobotstxtServer robotstxtServer) {
        this.robotstxtServer = robotstxtServer;
    }*/

    public Frontier getFrontier() {
        return frontier;
    }

    public void setFrontier(Frontier frontier) {
        this.frontier = frontier;
    }

    public UrlIdServer getDocIdServer() {
        return docIdServer;
    }

    public void setDocIdServer(UrlIdServer docIdServer) {
        this.docIdServer = docIdServer;
    }

    public Object getCustomData() {
        return customData;
    }

    public void setCustomData(Object customData) {
        this.customData = customData;
    }

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
        String folder = "./data/crawl";
        int numofCrawlers = 5;
        int maxDepth = 1;



        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(folder);
        config.setMaxDepthOfCrawling(maxDepth);

        PageFetcher pageFetcher = new PageFetcher(config);
        Controller controller = new Controller(config, pageFetcher);

        controller.addSeed("http://starwars.wikia.com/wiki/Yoda");
        controller.addSeed("http://starwars.wikia.com/wiki/Anakin_Skywalker");
        controller.addSeed("http://starwars.wikia.com/wiki/Qui-Gon_Jinn");
        //controller.addSeed("http://www.ics.uci.edu/~welling/");
        //controller.addSeed("http://www.ics.uci.edu/");

        /*
         * Start the crawl. This is a blocking operation, meaning that your code
         * will reach the line after this only when crawling is finished.
         */
        controller.start(WebCrawler.class, numofCrawlers);



    }

}
