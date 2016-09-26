package observo;

import observo.conf.ObservoConf;
import observo.utils.Serializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ObservableImpl<T extends Serializable> implements Observable<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservableImpl.class);

    private final CuratorFramework client;
    private final ObservoConf observoConf;
    private final String hostname;
    private final String path;
    private final String observersPath;
    private final Class<T> dataType;
    private final Map<Observer<T>, ObserverWatcher> observers = new ConcurrentHashMap<>();
    private final InterProcessSemaphoreMutex lock;

    public ObservableImpl(CuratorFramework client, ObservoConf observoConf, String hostname, String path, Class<T> dataType) {
        this.client = client;
        this.observoConf = observoConf;
        this.hostname = hostname;
        this.path = path;
        this.observersPath = path + "/observers";
        this.dataType = dataType;
        this.lock = new InterProcessSemaphoreMutex(client, path + "/lock");
        createObserversPathIfItDoesNotExists();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                unregisterAllObservers();
            }
        });
    }

    private void createObserversPathIfItDoesNotExists() {
        try {
            if (client.checkExists().forPath(observersPath) == null) {
                client.create().creatingParentsIfNeeded().forPath(observersPath);
            }
        } catch (Exception e) {
            LOGGER.error("could not create observers path: {}", e);
        }
    }

    Set<Observer<T>> getObservers() {
        return observers.keySet();
    }

    @Override
    public void registerObserver(Observer<T> observer) {
        try {
            // 1. acquire lock
            boolean acquired = lock.acquire(observoConf.getLockTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                LOGGER.error("could not acquire the lock within {} ms", observoConf.getLockTimeoutMs());
            }

            // 2. create observer if does not exists
            String childPath = generateUniqueChildPath();
            ObserverWatcher observerWatcher = new ObserverWatcher(client, path, childPath, observer, dataType);

            // 3. set watcher on data
            observers.put(observer, observerWatcher);
            client.getData().usingWatcher(observerWatcher).forPath(path);

            LOGGER.debug("{} registered", observer);

        } catch(Exception e) {
            LOGGER.error("Exception while registering observer: {} {}", observer, e);

        } finally {
            try {
                // 4. release lock
                lock.release();
            } catch (Exception e) {
                LOGGER.error("Exception while releasing the lock: {}", e);
            }
        }

    }

    @Override
    public void unregisterObserver(Observer<T> observer) {
        try {
            // 1. acquire lock
            boolean acquired = lock.acquire(observoConf.getLockTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                LOGGER.error("could not acquire the lock within {} ms", observoConf.getLockTimeoutMs());
            }

            // 2. delete observer node
            ObserverWatcher observerWatcher = observers.remove(observer);
            if (observerWatcher == null) {
                LOGGER.error("the observer {} was found within the list of registered observers {}", observer, observers.keySet());

            } else {
                // 3. disable watcher
                observerWatcher.disable();
            }

            LOGGER.debug("{} unregistered", observer);

        } catch(Exception e) {
            LOGGER.error("Exception while unregistering observer: {} {}", observer, e);

        } finally {
            try {
                // 4. release lock
                lock.release();
            } catch (Exception e) {
                LOGGER.error("Exception while releasing the lock: {}", e);
            }
        }

    }

    @Override
    public void unregisterAllObservers() {
        observers.keySet().forEach(observer -> unregisterObserver(observer));
    }

    @Override
    public void notifyObservers() {
        notifyObservers(null);
    }

    @Override
    public void notifyObservers(T data) {
        try {
            // 1. acquire lock
            boolean acquired = lock.acquire(observoConf.getLockTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                LOGGER.error("could not acquire the lock within {} ms", observoConf.getLockTimeoutMs());
            }

            // 2. get children and set watchers
            List<String> children = client.getChildren().forPath(observersPath);
            LOGGER.debug("childrens: {}", children);

            CountDownLatch latch = new CountDownLatch(children.size());
            CuratorWatcher childNotifiedWatcher = event -> {
                LOGGER.debug("child data updated: {}", event);
                latch.countDown();
            };

            for (String child: children) {
                client.getData().usingWatcher(childNotifiedWatcher).forPath(observersPath + "/" + child);
            }

            // 3. update data
            client.setData().forPath(path, Serializer.serialize(data));

            // 4. await for all the child watchers to fire
            boolean notified = latch.await(observoConf.getNotificationTimeoutMs(), TimeUnit.MILLISECONDS);
            if (notified) {
                LOGGER.info("observers were successfully notified");
            } else {
                LOGGER.error("could not notify all the observers within {} ms", observoConf.getNotificationTimeoutMs());
            }

        } catch(Exception e) {
            LOGGER.error("Exception while notifying observers: {}", e);

        } finally {
            try {
                // 5. release lock
                lock.release();
            } catch (Exception e) {
                LOGGER.error("Exception while releasing the lock: {}", e);
            }
        }
    }

    private String generateUniqueChildPath() {
        return observersPath + "/" + hostname + observers.size();
    }

}

