package observo;

import observo.conf.ZookeeperConf;
import observo.utils.HostnameProvider;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ObservableImplITest {

    private static TestingServer zkServer;
    private static final int RETRY_TIMES = 4;
    private static final int RETRY_MS_SLEEP = 1000;
    private static final String NAME_SPACE_SUFFIX = "testApp";
    private static Observable<News> newsFeeds;
    private static ObservableFactory factory;

    @BeforeClass
    public static void setUpClass() throws Exception {
        zkServer = new TestingServer();
        ZookeeperConf zookeeperConf = new ZookeeperConf(zkServer.getConnectString(), RETRY_TIMES, RETRY_MS_SLEEP);
        HostnameProvider hostnameProvider = () -> "hostname";
        factory = new ObservableFactory(zookeeperConf, NAME_SPACE_SUFFIX, hostnameProvider);
    }

    @Before
    public void setUp() {
        newsFeeds = factory.createObservable("news", News.class);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        zkServer.stop();
    }

    @Test
    public void registerShouldRegisterAnObserver() {
        System.out.println(Thread.currentThread().getContextClassLoader().getResource("."));
        System.out.println(Thread.currentThread().getContextClassLoader().getResource("log4j.properties"));

        Observer<News> observer = data -> {};
        newsFeeds.registerObserver(observer);

        Set<Observer<News>> expected = new HashSet<>();
        expected.add(observer);

        assertThat(((ObservableImpl)newsFeeds).getObservers(), is(expected));
    }

    @Test
    public void unregisterShouldUnregisterAnObserver() {
        Observer<News> observer = data -> {};
        newsFeeds.registerObserver(observer);
        newsFeeds.unregisterObserver(observer);

        Set<Observer<News>> expected = new HashSet<>();
        assertThat(((ObservableImpl)newsFeeds).getObservers(), is(expected));
    }

    @Test
    public void notifyShouldNotifyAnEmptyNews() {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObservers();

        assertThat(observer.isNotified(), is(true));
        assertThat(observer.getData(), is(nullValue()));
    }

    @Test
    public void notifyShouldNotifyNews() {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        News news = new News("title", "content");
        newsFeeds.notifyObservers(news);

        assertThat(observer.isNotified(), is(true));
        assertThat(observer.getData(), is(news));
    }

    @Test
    public void notifyShouldNotifyMultipleNews() {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);

        News news1 = new News("news1", "content");
        newsFeeds.notifyObservers(news1);
        assertThat(observer.getData(), is(news1));

        News news2 = new News("news2", "content");
        newsFeeds.notifyObservers(news2);
        assertThat(observer.getData(), is(news2));

        newsFeeds.notifyObservers();
        assertThat(observer.getData(), is(nullValue()));
    }

    @Test
    public void notifyShouldNotifyToMultipleObservers() {
        TestObserver<News> observer1 = new TestObserver<>();
        newsFeeds.registerObserver(observer1);

        TestObserver<News> observer2 = new TestObserver<>();
        newsFeeds.registerObserver(observer2);

        News news = new News("title", "content");
        newsFeeds.notifyObservers(news);

        assertThat(observer1.getData(), is(news));
        assertThat(observer2.getData(), is(news));
    }

    @Test
    public void nofityShouldNotNotifyUnregisteredObservers() {
        TestObserver<News> observer1 = new TestObserver<>();
        newsFeeds.registerObserver(observer1);

        TestObserver<News> observer2 = new TestObserver<>();
        newsFeeds.registerObserver(observer2);
        newsFeeds.unregisterObserver(observer2);

        newsFeeds.notifyObservers();

        assertThat(observer1.isNotified(), is(true));
        assertThat(observer2.isNotified(), is(false));
    }

    private static class TestObserver<T> implements Observer<T> {

        private volatile T data;
        private volatile boolean notified = false;

        @Override
        public void update(T data) {
            this.data = data;
            this.notified = true;
        }

        public T getData() {
            return data;
        }

        public boolean isNotified() {
            return notified;
        }
    }

    private static class News implements Serializable {
        private final String title;
        private final String content;

        public News(String title, String content) {
            this.title = title;
            this.content = content;
        }

        @Override
        public String toString() {
            return "News{" +
                    "title='" + title + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof News)) return false;

            News news = (News) o;

            if (title != null ? !title.equals(news.title) : news.title != null) return false;
            return content != null ? content.equals(news.content) : news.content == null;

        }

        @Override
        public int hashCode() {
            int result = title != null ? title.hashCode() : 0;
            result = 31 * result + (content != null ? content.hashCode() : 0);
            return result;
        }
    }

}
