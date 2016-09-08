package observo;

import observo.conf.ZookeeperConf;
import observo.utils.CurrentTimeProvider;
import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ObservableFactoryITest {

    private static TestingServer zkServer;
    private static ObservableFactory factory;

    private static final int RETRY_TIMES = 4;
    private static final int RETRY_MS_SLEEP = 1000;
    private static final String NAME_SPACE_SUFFIX = "test";
    private static final int COOL_DOWN_PERIOD_MS = 100;

    @BeforeClass
    public static void setUp() throws Exception {
        zkServer = new TestingServer();
        ZookeeperConf zookeeperConf = new ZookeeperConf(zkServer.getConnectString(), RETRY_TIMES, RETRY_MS_SLEEP);
        factory = new ObservableFactory(zookeeperConf, NAME_SPACE_SUFFIX, COOL_DOWN_PERIOD_MS, new CurrentTimeProvider());
    }

    @AfterClass
    public static void tearDown() throws IOException {
        zkServer.stop();
    }

    @Test
    public void stringDataObservers() throws Exception {
        Observable<String> stringObservable = factory.createObservable("string", String.class);

        TestStringObserver first = new TestStringObserver();
        TestStringObserver second = new TestStringObserver();
        TestStringObserver third = new TestStringObserver();

        stringObservable.registerObserver(first);
        stringObservable.registerObserver(second);
        stringObservable.registerObserver(third);

        stringObservable.notifyObservers();
        stringObservable.notifyObservers("this is");
        stringObservable.notifyObservers();
        stringObservable.notifyObservers("a string");

        waitForMs(500);

        String expected = "nullthis isnulla string";
        assertThat(first.acc, is(expected));
        assertThat(second.acc, is(expected));
        assertThat(third.acc, is(expected));

    }

    @Test
    public void mixedDataObservers() throws Exception {
        Observable<String> stringObservable = factory.createObservable("string", String.class);
        Observable<Empty> emptyObservable = factory.createObservable("empty", Empty.class);
        Observable<Person> personObservable = factory.createObservable("person", Person.class);

        TestStringObserver string1Observer = new TestStringObserver();
        TestStringObserver string2Observer = new TestStringObserver();
        TestEmptyObserver empty1Observer = new TestEmptyObserver();
        TestEmptyObserver empty2Observer = new TestEmptyObserver();
        TestPersonObserver personObserver = new TestPersonObserver();

        stringObservable.registerObserver(string1Observer);
        stringObservable.registerObserver(string2Observer);
        emptyObservable.registerObserver(empty1Observer);
        emptyObservable.registerObserver(empty2Observer);
        personObservable.registerObserver(personObserver);

        Person person = new Person("John", 34);

        emptyObservable.notifyObservers();
        stringObservable.notifyObservers("this is");

        personObservable.notifyObservers(person);
        emptyObservable.notifyObservers();
        stringObservable.notifyObservers(" a string");
        emptyObservable.notifyObservers();

        waitForMs(500);

        String stringExpected = "this is a string";
        assertThat(string1Observer.acc, is(stringExpected));
        assertThat(string2Observer.acc, is(stringExpected));

        assertThat(empty1Observer.counter, is(3));
        assertThat(empty2Observer.counter, is(3));

        assertThat(personObserver.data, is(person));

    }

    private static class TestStringObserver implements Observer<String> {

        public volatile String acc = "";

        @Override
        public void update(String data) {
            acc += data;
        }
    }

    private static class TestEmptyObserver implements Observer<Empty> {

        public int counter = 0;

        @Override
        public void update(Empty data) {
            counter++;
        }
    }

    private static class TestPersonObserver implements Observer<Person> {

        public volatile Person data;

        @Override
        public void update(Person data) {
            this.data = data;
        }
    }

    private static class Person implements Serializable {

        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;

            Person person = (Person) o;

            if (age != person.age) return false;
            return name != null ? name.equals(person.name) : person.name == null;

        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + age;
            return result;
        }
    }

    private static void waitForMs(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
