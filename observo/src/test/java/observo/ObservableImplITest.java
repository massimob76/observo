package observo;

import observo.conf.ObservoConf;
import observo.conf.ZookeeperConf;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.Collections.emptySet;
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
    private static final News TEST_NEWS = new News("title", "content");

    private static TestingServer zkServer;
    private static Observable<News> newsFeeds;

    private TestCompleteTask completeTask = new TestCompleteTask();
    private TestErrorTask errorTask = new TestErrorTask();

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
    public void unregisterAllObserversShouldUnregisterAllObservers() {
        Observer<News> observer1 = data -> {};
        Observer<News> observer2 = data -> {};
        newsFeeds.registerObserver(observer1);
        newsFeeds.registerObserver(observer2);
        newsFeeds.unregisterAllObservers();
        assertThat(((ObservableImpl)newsFeeds).getObservers(), is(emptySet()));

    }

    @Test
    public void notifyObserversAsyncShouldNotifyAsynchroneously() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObserversAsync();

        assertThat(observer.awaitForNotification(), is(true));
    }

    @Test
    public void notifyObserversAsyncShouldNotifyDataAsynchroneously() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObserversAsync(TEST_NEWS);

        assertThat(observer.awaitForNotification(), is(true));
        assertThat(observer.getData(), is(TEST_NEWS));
    }

    @Test
    public void notifyObserversShouldNotifySynchroneously() throws InterruptedException, ExecutionException, TimeoutException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObservers();
        assertThat(observer.isNotified(), is(true));
    }

    @Test
    public void notifyObserversShouldNotifyDataSynchroneously() throws InterruptedException, ExecutionException, TimeoutException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObservers(TEST_NEWS);
        assertThat(observer.isNotified(), is(true));
        assertThat(observer.getData(), is(TEST_NEWS));
    }

    @Test
    public void notifyObserversShouldNotifyMultipleNews() throws InterruptedException, TimeoutException, ExecutionException {
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
    public void notifyShouldNotifyToMultipleObservers() throws InterruptedException, TimeoutException, ExecutionException {
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
    public void notifyShouldNotNotifyUnregisteredObservers() throws InterruptedException, TimeoutException, ExecutionException {
        TestObserver<News> observer1 = new TestObserver<>();
        newsFeeds.registerObserver(observer1);

        TestObserver<News> observer2 = new TestObserver<>();
        newsFeeds.registerObserver(observer2);
        newsFeeds.unregisterObserver(observer2);

        newsFeeds.notifyObservers();

        assertThat(observer1.isNotified(), is(true));
        assertThat(observer2.isNotified(), is(false));
    }

    @Test
    public void executesCompleteTaskAfterNotification() throws InterruptedException {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObserversAsync()
                .whenComplete(completeTask)
                .whenError(errorTask);

        assertThat(completeTask.waitUntilCompletion(), is(true));
        assertThat(errorTask.waitUntilCompletion(), is(false));
    }

    @Test
    public void executesErrorTaskIfSomethingGoesWrong() throws Exception {
        TestObserver<News> observer = new TestObserver<>();
        newsFeeds.registerObserver(observer);

        zkServer.stop();

        newsFeeds.registerObserver(observer);
        newsFeeds.notifyObserversAsync()
                .whenComplete(completeTask)
                .whenError(errorTask);

        assertThat(completeTask.waitUntilCompletion(), is(false));
        assertThat(errorTask.waitUntilCompletion(), is(true));
        assertThat(errorTask.getThrowable().getMessage(), is("KeeperErrorCode = ConnectionLoss"));
    }

    private static class TestCompleteTask implements Runnable {

        private CountDownLatch runLatch = new CountDownLatch(1);

        public boolean waitUntilCompletion() throws InterruptedException {
            return runLatch.await(500, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            runLatch.countDown();
        }
    }

    private static class TestErrorTask implements Consumer<Throwable> {

        private CountDownLatch runLatch = new CountDownLatch(1);
        private Throwable throwable = null;

        public boolean waitUntilCompletion() throws InterruptedException {
            return runLatch.await(500, TimeUnit.MILLISECONDS);
        }

        @Override
        public void accept(Throwable throwable) {
            this.throwable = throwable;
            runLatch.countDown();
        }

        public Throwable getThrowable() {
            return throwable;
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

        public boolean isNotified() {
            return notified.getCount() == 0;
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
