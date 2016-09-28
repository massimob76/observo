package observoexample.services;

import observoexample.news.News;
import observoexample.news.NewsObserver;
import observo.Observable;
import observo.ObservableFactory;
import observo.conf.ObservoConf;
import observo.conf.ZookeeperConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewsService {

    private static final int RETRY_TIMES = 1;
    private static final int RETRY_MS_SLEEP = 100;
    private static final long NOTIFICATION_TIMEOUT_MS = 100;
    private static final long LOCK_TIMEOUT_MS = 100;
    private static final String DEFAULT_CONNECTION_STRING = "localhost:2181";
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsService.class);

    private final Observable<News> observable;
    private final NewsObserver newsObserver;
    private final NewsObserver newsSecondObserver;

    public NewsService() {
        ZookeeperConf zookeeperConf = new ZookeeperConf(getZkConnectionString(), RETRY_TIMES, RETRY_MS_SLEEP);
        ObservoConf observoConf = new ObservoConf(NOTIFICATION_TIMEOUT_MS, LOCK_TIMEOUT_MS);
        String nameSpaceSuffix = "observo-example";
        ObservableFactory observableFactory = ObservableFactory.instance(zookeeperConf, observoConf, nameSpaceSuffix);
        observable = observableFactory.createObservable("news", News.class);

        newsObserver = new NewsObserver();
        observable.registerObserver(newsObserver);

        newsSecondObserver = new NewsObserver();
        observable.registerObserver(newsSecondObserver);
    }

    private static String getZkConnectionString() {
        String zkConnectionString = System.getProperty("zkConnectionString", DEFAULT_CONNECTION_STRING);
        LOGGER.info("using zookeeper connection string {}", zkConnectionString);
        return zkConnectionString;
    }

    public NewsObserver getNewsObserver() {
        return newsObserver;
    }

    public NewsObserver getNewsSecondObserver() {
        return newsSecondObserver;
    }

    public void publish(News news) {
        observable.notifyObservers(news);
    }
}
