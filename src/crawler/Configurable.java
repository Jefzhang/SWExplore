package crawler;

/**
 * Created by jfzhang on 02/03/2017.
 */
public abstract class Configurable {

    protected CrawlConfig config;

    protected Configurable(CrawlConfig config) {
        this.config = config;
    }

    public CrawlConfig getConfig() {
        return config;
    }
}
