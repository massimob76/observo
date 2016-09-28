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

        lockedBlock(() -> {

            try {

                // create observer if does not exists
                String childPath = generateUniqueChildPath();

                // create and set watcher
                ObserverWatcher observerWatcher = new ObserverWatcher(client, path, childPath, observer, dataType);
                observers.put(observer, observerWatcher);

                LOGGER.debug("{} registered", observer);

            } catch(Exception e) {
                LOGGER.error("Exception while registering observer: {} {}", observer, e);
            }

        });

    }

    @Override
    public void unregisterObserver(Observer<T> observer) {
        lockedBlock(() -> unregistering(observer));
    }

    @Override
    public void unregisterAllObservers() {
        lockedBlock(() -> observers.keySet().forEach(observer -> unregistering(observer)));
    }

    private void unregistering(Observer observer) {
        try {

            // delete observer node
            ObserverWatcher observerWatcher = observers.remove(observer);
            if (observerWatcher == null) {
                LOGGER.error("the observer {} was not found within the list of registered observers {}", observer, observers.keySet());

            } else {
                // disable watcher
                observerWatcher.disable();
            }

            LOGGER.debug("{} unregistered", observer);

        } catch(Exception e) {
            LOGGER.error("Exception while unregistering observer: {} {}", observer, e);
        }

    }

    @Override
    public void notifyObservers() {
        notifyObservers(null);
    }

    @Override
    public void notifyObservers(T data) {

        lockedBlock(() -> {

            try {

                // get children and set watchers
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

                // update data
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
            }

        });
    }

    private void lockedBlock(Runnable runnable) {
        try {
            // acquire lock
            boolean acquired = lock.acquire(observoConf.getLockTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                LOGGER.error("could not acquire the lock within {} ms", observoConf.getLockTimeoutMs());
            }

            // run
            runnable.run();

        } catch (Exception e) {
            LOGGER.error("Exception while acquiring the lock: {}", e);

        } finally {
            try {
                // release lock
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

