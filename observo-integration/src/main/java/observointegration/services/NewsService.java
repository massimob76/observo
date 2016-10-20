package observointegration.services;

import observo.Observable;
import observo.ObservableFactory;
import observo.conf.ObservoConf;
import observo.conf.ZookeeperConf;
import observointegration.news.News;
import observointegration.news.NewsObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class NewsService {

    private static final int RETRY_TIMES = 1;
    private static final int RETRY_MS_SLEEP = 100;
    private static final long NOTIFICATION_TIMEOUT_MS = 100;
    private static final long LOCK_TIMEOUT_MS = 100;
    private static final String DEFAULT_CONNECTION_STRING = "localhost:2181";
    private static final int CONNECTION_TIMEOUT_MS = 100;
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsService.class);

    private final Observable<News> observable;
    private final NewsObserver newsObserver;
    private final NewsObserver newsSecondObserver;

    public NewsService() {
        ZookeeperConf zookeeperConf = new ZookeeperConf(getZkConnectionString(), CONNECTION_TIMEOUT_MS, RETRY_TIMES, RETRY_MS_SLEEP);
        ObservoConf observoConf = new ObservoConf(NOTIFICATION_TIMEOUT_MS, LOCK_TIMEOUT_MS);
        String nameSpaceSuffix = "observo-integration";
        ObservableFactory observableFactory = new ObservableFactory(zookeeperConf, observoConf, nameSpaceSuffix);
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

    public void publishAsynch(News news) {
        observable.notifyObservers(news);
    }

    public void publishSynch(News news) {
        CountDownLatch latch = new CountDownLatch(1);
        Runnable onSuccess = () -> LOGGER.info("news {} published to all observers", news);
        Runnable onError = () -> LOGGER.error("failure in publishing news {}", news);
        Runnable onCompletion = () -> latch.countDown();
        observable.notifyObservers(news, onSuccess, onError, onCompletion);
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("Synch publishing was interrupted before all observers were notified");
        }
    }
}
