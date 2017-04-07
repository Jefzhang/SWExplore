package crawler;

import fetcher.PageFetcher;
import jdk.nashorn.internal.parser.JSONParser;
import multithread.*;
import multithread.WebUrlQueues;
import org.json.simple.JSONValue;
import url.URLnormlization;
import url.WebURL;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
/**
 * Created by jfzhang on 05/03/2017.
 */
class webUrlComparator implements Comparator<WebURL>{
    @Override
    public int compare(WebURL url1,WebURL url2){
        int depth1 = url1.getDepth();
        int depth2 = url2.getDepth();
        String name1 = url1.getAnchor();
        String name2 = url2.getAnchor();
        if(depth1<depth2){
            return -1;
        }
        else if(depth1==depth2){
            return name1.compareTo(name2);
        }
        else return 1;

    }
}
public class SimpleController extends Configurable{


    static final Logger logger = LoggerFactory.getLogger(SimpleController.class);
    protected boolean finished;

    protected TreeSet<WebURL> crawlersLocalData = new TreeSet<>(new webUrlComparator());
   // protected ArrayList<WebURL> crawlersLocalData = new ArrayList<>();

    /**
     * Is the crawling session set to 'shutdown'. Crawler threads monitor this
     * flag and when it is set they will no longer process new pages.
     */
    protected boolean shuttingDown;

    protected PageFetcher pageFetcher;
    protected WebUrlQueues<WebURL> queue;
    protected UrlDiscovered urlDiscovered;
    protected int numPagesVisited;
    protected double crawTime;
   //public ArrayList<String> message;

    public SimpleController(CrawlConfig config, PageFetcher pageFetcher, String queueType) {
        super(config);
        //config.validate();
        urlDiscovered = new SafeHashSet();
        if(queueType.equals("LockFreeQueue")){
            queue = new LockFreeQueue<WebURL>();
        }else if(queueType.equals("BlockingQueue")){
            queue = new SimpleBlockingQueue<WebURL>();
        }else if(queueType.equals("Java-BlockingQueue")){
            queue = new JavaBlockingQueue<>();
        }else{
            queue = new JavaConcurrentQueue<>();
        }
        this.pageFetcher = pageFetcher;
        finished = false;
        shuttingDown = false;
        crawTime = 0;
        numPagesVisited = 0;
      //  message = new ArrayList<>();
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


            finished = false;
            crawlersLocalData.clear();
            if(this.config.getResumable()){                    //load all the urls not-visited of last time
                Object obj = JSONValue.parse(new FileReader(config.getCrawlStorageFolder()+"lastTime.json"));
                JSONObject resumeData = (JSONObject) obj;
                JSONArray queueData = (JSONArray) resumeData.get("queue");
                JSONArray visited = (JSONArray) resumeData.get("visited");

                Iterator<Object> ite = queueData.iterator();
                while(ite.hasNext()){
                    Object data = ite.next();
                    JSONObject dataJson = (JSONObject) data;
                    WebURL url = new WebURL();
                    url.setURL((String)dataJson.get("url"));
                    System.out.println((String)dataJson.get("url"));
                    Long depth = (Long) dataJson.get("depth");
                    url.setDepth(depth.shortValue());
                    url.setParentAnchor((String)dataJson.get("parentAnchor"));
                    queue.schedule(url);
                }

                Iterator<Object> ite1 = visited.iterator();
                while(ite1.hasNext()){
                    String href = (String)ite1.next();
                    urlDiscovered.add(href);
                }
                System.out.println("Successfully load data of the last time from lastTime.json");
            }
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

            long before = System.nanoTime();
            final SimpleController controller = this;
            final CrawlConfig config = this.getConfig();

            Thread monitorThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (true) {
                                Thread.sleep(config.getThreadMonitoringDelaySeconds());
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
                                    Thread.sleep(config.getThreadShutdownDelaySeconds());

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
                                            if (!queue.isEmpty()) {
                                                continue;
                                            }
                                            logger.info(
                                                    "No thread is working and no more URLs are in " +
                                                            "queue waiting for another " +
                                                            config.getThreadShutdownDelaySeconds() +
                                                            " seconds to make sure...");
                                            Thread.sleep(config.getThreadShutdownDelaySeconds());
                                            //queueLength = frontier.getQueueLength();
                                            if (!queue.isEmpty()) {
                                                continue;
                                            }
                                        }

                                        logger.info(
                                                "All of the crawlers are stopped. Finishing the " +
                                                        "process...");
                                        // At this step, frontier notifies the threads that were
                                        // waiting for new URLs and they should stop
                                        crawTime = (System.nanoTime() - before) / 1_000_000_000.0;
                                        queue.finish();   //finish workqueue, the crawlers will shutdown


                                        if(shuttingDown&&!queue.isEmpty()){          //store the urls not visited for the next time
                                            JSONObject resumeData = new JSONObject();
                                            JSONArray queueData = new JSONArray();
                                            WebURL data = queue.getNextURL();
                                            while(data!=null){
                                                JSONObject urlJson  = new JSONObject();
                                                urlJson.put("url",data.getURL());
                                                urlJson.put("depth",data.getDepth());
                                                urlJson.put("parentAnchor",data.getParentAnchor());
                                                queueData.add(urlJson);
                                                data = queue.getNextURL();
                                            }

                                            JSONArray baseData = new JSONArray();
                                            Iterator<String> ite = urlDiscovered.getURLBase().iterator();
                                            while(ite.hasNext()){
                                                String href = ite.next();
                                                baseData.add(href);
                                            }
                                            resumeData.put("queue",queueData);
                                            resumeData.put("visited",baseData);
                                            try (FileWriter file1 = new FileWriter(controller.getConfig().getCrawlStorageFolder()+"lastTime.json")) {
                                                file1.write(resumeData.toJSONString());
                                                System.out.println("Successfully stored urls to File lastTime.json");
                                            }
                                        }

                                        for (T crawler : crawlers) {
                                            numPagesVisited +=crawler.getNumPagesVisited();
                                            crawlersLocalData.addAll(crawler.getMyLocalData());
                                        }


                                        FileWriter file = new FileWriter(controller.getConfig().getCrawlStorageFolder()+"output.txt",true);
                                        BufferedWriter write = new BufferedWriter(file);
                                        for (WebURL webURL : crawlersLocalData){
                                            write.write(webURL.toString());
                                        }
                                        write.close();
                                        file.close();

                                        JSONObject resultJson = new JSONObject();
                                        JSONArray nodes;
                                        JSONArray links;

                                        if(controller.getConfig().getResumable()){
                                            Object graph = JSONValue.parse(new FileReader(controller.getConfig().getCrawlStorageFolder()+"resultJson.json"));
                                            JSONObject graphJson = (JSONObject) graph;
                                            nodes = (JSONArray) graphJson.get("nodes");
                                            links = (JSONArray) graphJson.get("links");
                                        }else{
                                            nodes = new JSONArray();
                                            links = new JSONArray();
                                        }
                                        for (WebURL webURL : crawlersLocalData){
                                            JSONObject node = new JSONObject();
                                            node.put("id",webURL.getAnchor());
                                            node.put("depth",webURL.getDepth());
                                            node.put("url",webURL.getURL());
                                            nodes.add(node);
                                            if(webURL.getDepth()!=0){
                                                JSONObject link = new JSONObject();
                                                link.put("source",webURL.getParentAnchor());
                                                link.put("target",webURL.getAnchor());
                                                links.add(link);
                                            }
                                        }
                                        resultJson.put("nodes",nodes);
                                        resultJson.put("links",links);
                                        try (FileWriter file1 = new FileWriter(controller.getConfig().getCrawlStorageFolder()+"resultJson.json")) {
                                            file1.write(resultJson.toJSONString());
                                            System.out.println("Successfully wrote characters found to resultJson.json and output.txt");
                                        }
                                        System.out.format("Process finished within %fs.\n", crawTime);
                                        System.out.format("Visited %d pages and got %d new characters\n",numPagesVisited,crawlersLocalData.size());
                                        logger.info(
                                                "Waiting for " + config.getCleanupDelaySeconds() +
                                                        " seconds before final clean up...");
                                        Thread.sleep(config.getCleanupDelaySeconds());

                                        pageFetcher.shutDown();

                                        finished = true;
                                        break;

                                        //return;
                                    }
                                }
                            }
                    } catch (Exception e) {
                        logger.error("Unexpected Error", e);
                    }
                }
            });

            monitorThread.start();

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


    public void addSeed(String pageUrl) {
        String canonicalUrl = URLnormlization.getCanonicalURL(pageUrl);
        if (canonicalUrl == null) {
            logger.error("Invalid seed URL: {}", pageUrl);
        } else {
            if (this.urlDiscovered.contains(pageUrl)) {
                    logger.trace("This URL is already seen.");
                    return;
            } else {
                try {
                    this.urlDiscovered.add(canonicalUrl);
                } catch (Exception e) {
                    logger.error("Could not add seed: {}", e.getMessage());
                }
            }

            WebURL webUrl = new WebURL();
            webUrl.setURL(canonicalUrl);
            webUrl.setDepth((short) 0);
            this.queue.schedule(webUrl);
        }
    }


    public PageFetcher getPageFetcher() {
        return pageFetcher;
    }

    public void setPageFetcher(PageFetcher pageFetcher) {
        this.pageFetcher = pageFetcher;
    }


    public WebUrlQueues getFrontier() {
        return this.queue;
    }


    public UrlDiscovered getUrlBase() {
        return this.urlDiscovered;
    }

    public double getCrawTime(){
        return this.crawTime;
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
        queue.finish();
    }

}
