package observo;

import observo.conf.ObservoConf;
import observo.conf.ZookeeperConf;
import org.apache.curator.test.TestingServer;
import org.junit.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ObservableImplITest {

    private static final long NOTIFICATION_TIMEOUT_MS = 300;
    private static final long LOCK_TIMEOUT_MS = 1000;
    private static final int RETRY_TIMES = 1;
    private static final int RETRY_MS_SLEEP = 10;
    private static final String NAME_SPACE_SUFFIX = "testApp";
    private static final int CONNECTION_TIMEOUT_MS = 200;

    private static TestingServer zkServer;
    private static Observable<News> newsFeeds;

    private TestRunnable onSuccess = new TestRunnable();
    private TestRunnable onError = new TestRunnable();
    private TestRunnable onCompletion = new TestRunnable();

    @Before
    public void setUp() throws Exception {
        zkServer = new TestingServer();
        ZookeeperConf zookeeperConf = new ZookeeperConf(zkServer.getConnectString(), CONNECTION_TIMEOUT_MS, RETRY_TIMES, RETRY_MS_SLEEP);
        ObservoConf observoConf = new ObservoConf(NOTIFICATION_TIMEOUT_MS, LOCK_TIMEOUT_MS);
        ObservableFactory factory = new ObservableFactory(zookeeperConf, observoConf, NAME_SPACE_SUFFIX);
        newsFeeds = factory.createObservable("news", News.class);
    }

    @After
    public void tearDown() throws IOException {
        newsFeeds.unregisterAllObservers();
        zkServer.stop();
    }

    @Test
    public void registerShouldRegisterAnObserver() {
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
    public void notifyShouldNotifyAnEmptyNews() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObservers();

        assertThat(observer.awaitForNotification(), is(true));
        assertThat(observer.getData(), is(nullValue()));
    }

    @Test
    public void notifyShouldNotifyNews() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        News news = new News("title", "content");
        newsFeeds.notifyObservers(news);

        assertThat(observer.awaitForNotification(), is(true));
        assertThat(observer.getData(), is(news));
    }

    @Test
    public void notifyShouldNotifyMultipleNews() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);

        News news1 = new News("news1", "content");
        newsFeeds.notifyObservers(news1);
        observer.awaitForNotification();
        assertThat(observer.getData(), is(news1));

        News news2 = new News("news2", "content");
        newsFeeds.notifyObservers(news2);
        observer.awaitForNotification();
        assertThat(observer.getData(), is(news2));

        newsFeeds.notifyObservers();
        observer.awaitForNotification();
        assertThat(observer.getData(), is(nullValue()));
    }

    @Test
    public void notifyShouldNotifyToMultipleObservers() throws InterruptedException {
        TestObserver<News> observer1 = new TestObserver<>();
        newsFeeds.registerObserver(observer1);

        TestObserver<News> observer2 = new TestObserver<>();
        newsFeeds.registerObserver(observer2);

        News news = new News("title", "content");
        newsFeeds.notifyObservers(news);

        observer1.awaitForNotification();
        assertThat(observer1.getData(), is(news));

        observer2.awaitForNotification();
        assertThat(observer2.getData(), is(news));
    }

    @Test
    public void notifyShouldNotNotifyUnregisteredObservers() throws InterruptedException {
        TestObserver<News> observer1 = new TestObserver<>();
        newsFeeds.registerObserver(observer1);

        TestObserver<News> observer2 = new TestObserver<>();
        newsFeeds.registerObserver(observer2);
        newsFeeds.unregisterObserver(observer2);

        newsFeeds.notifyObservers();

        assertThat(observer1.awaitForNotification(), is(true));
        assertThat(observer2.awaitForNotification(), is(false));
    }

    @Test
    public void callsOnSuccessAndOnCompletionAfterNotification() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObservers(onSuccess, onError, onCompletion);

        assertThat(observer.awaitForNotification(), is(true));

        assertThat(onSuccess.waitUntilCompletion(), is(true));
        assertThat(onError.waitUntilCompletion(), is(false));
        assertThat(onCompletion.waitUntilCompletion(), is(true));
    }

    @Test
    public void callsOnErrorAndOnCompletionIfSomethingGoesWrong() throws Exception {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);

        zkServer.stop();

        newsFeeds.notifyObservers(onSuccess, onError, onCompletion);

        assertThat(onSuccess.waitUntilCompletion(), is(false));
        assertThat(onError.waitUntilCompletion(), is(true));
        assertThat(onCompletion.waitUntilCompletion(), is(true));

    }

    private static class TestRunnable implements Runnable {

        private CountDownLatch runLatch = new CountDownLatch(1);

        public boolean waitUntilCompletion() throws InterruptedException {
            return runLatch.await(500, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            runLatch.countDown();
        }
    }

    private static class TestObserver<T> implements Observer<T> {

        private volatile T data;
        private CountDownLatch notified;

        public TestObserver() {
            resetNotified();
        }

        @Override
        public void update(T data) {
            this.data = data;
            this.notified.countDown();
        }

        public T getData() {
            return data;
        }

        public boolean awaitForNotification() throws InterruptedException {
            boolean isNotified = notified.await(500, TimeUnit.MILLISECONDS);
            resetNotified();
            return isNotified;
        }

        private void resetNotified() {
            notified = new CountDownLatch(1);
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
