package observo;

import observo.conf.ObservoConf;
import observo.lock.DistributedLock;
import observo.utils.Serializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObservableImpl<T extends Serializable> implements Observable<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObservableImpl.class);

    private final CuratorFramework client;
    private final ObservoConf observoConf;
    private final String hostname;
    private final String path;
    private final String observersPath;
    private final Class<T> dataType;
    private final Map<Observer<T>, ObserverWatcher> observers = new ConcurrentHashMap<>();
    private final DistributedLock distributedLock;

    public ObservableImpl(CuratorFramework client, ObservoConf observoConf, String hostname, String path, Class<T> dataType) {
        this.client = client;
        this.observoConf = observoConf;
        this.hostname = hostname;
        this.path = path;
        this.observersPath = path + "/observers";
        this.dataType = dataType;
        this.distributedLock = new DistributedLock(client, path + "/lock", observoConf.getLockTimeoutMs());
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

        distributedLock.lockedBlock(() -> {

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
        distributedLock.lockedBlock(() -> unregistering(observer));
    }

    @Override
    public void unregisterAllObservers() {
        if (observers.size() > 0) {
            distributedLock.lockedBlock(() -> observers.keySet().forEach(observer -> unregistering(observer)));
        }
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
        notifyObservers(data, null, null, null);
    }

    @Override
    public void notifyObservers(Runnable onSuccess, Runnable onError, Runnable onCompletion) {
        notifyObservers(null, onSuccess, onError, onCompletion);
    }

    @Override
    public void notifyObservers(T data, Runnable onSuccess, Runnable onError, Runnable onCompletion) {

        distributedLock.acquireLock();

        try {

            // get children and set watchers
            List<String> children = client.getChildren().forPath(observersPath);
            LOGGER.debug("childrens: {}", children);

            AfterNotificationAction afterNotificationAction = new AfterNotificationAction(
                    children.size(),
                    observoConf.getNotificationTimeoutMs(),
                    wrapWithReleaseLock(onSuccess),
                    wrapWithReleaseLock(onError),
                    onCompletion);

            CuratorWatcher childNotifiedWatcher = event -> {
                LOGGER.debug("child data updated: {}", event);
                afterNotificationAction.observerNotified();
            };

            for (String child : children) {
                client.getData().usingWatcher(childNotifiedWatcher).forPath(observersPath + "/" + child);
            }

            // update data
            client.setData().forPath(path, Serializer.serialize(data));

        } catch(Exception e) {
            LOGGER.error("exception while notifying observers: {}", e);
            distributedLock.releaseLock();

        }
    }

    private String generateUniqueChildPath() {
        return observersPath + "/" + hostname + observers.size();
    }

    private Runnable wrapWithReleaseLock(Runnable runnable) {
        return () -> {
            distributedLock.releaseLock();
            safeRun(runnable);
        };
    }

    private void safeRun(Runnable runnable) {
        if (runnable != null) {
            try {
                runnable.run();
            } catch (RuntimeException e) {
                LOGGER.error("Runnable threw exception {}", e);
            }
        }
    }

}

