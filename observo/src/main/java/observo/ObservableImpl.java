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
import java.util.concurrent.*;

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
                LOGGER.debug("{} unregistered", observer);
            }

        } catch(Exception e) {
            LOGGER.error("Exception while unregistering observer: {} {}", observer, e);
        }

    }

    @Override
    public void notifyObservers() throws InterruptedException, ExecutionException, TimeoutException {
        notifyObservers(null);
    }

    @Override
    public void notifyObservers(T data) throws InterruptedException, ExecutionException, TimeoutException {
        AsyncTask asyncTask = notifyObserversAsync(data);
        asyncTask.join(observoConf.getNotificationTimeoutMs(), TimeUnit.MILLISECONDS);
    }

    @Override
    public AsyncTask notifyObserversAsync() {
        return notifyObserversAsync(null);
    }

    @Override
    public AsyncTask notifyObserversAsync(T data) {

        AsyncTaskImpl asyncTask = new AsyncTaskImpl();
        distributedLock.acquireLock();

        try {

            // get observers
            List<String> observers = client.getChildren().forPath(observersPath);
            LOGGER.debug("observers: {}", observers);

            // set awaiting action for all observers to be notified

            CountDownLatch remainingToNotify = new CountDownLatch(observers.size());

            Runnable runnable = () -> {

                boolean notifiedToAll = false;
                try {
                    notifiedToAll = remainingToNotify.await(observoConf.getNotificationTimeoutMs(), TimeUnit.MILLISECONDS);

                    if (notifiedToAll) {
                        LOGGER.info("observers were successfully notified");
                        distributedLock.releaseLock();
                        asyncTask.completeSuccessfully();

                    } else {
                        String timeoutMsg = "could not notify all the observers within " + observoConf.getNotificationTimeoutMs() + " ms";
                        LOGGER.error(timeoutMsg);
                        asyncTask.completeExceptionally(new TimeoutException(timeoutMsg));
                    }

                } catch (InterruptedException e) {
                    LOGGER.error("Notification was interrupted", e);
                    Thread.currentThread().interrupt();
                    asyncTask.completeExceptionally(e);

                }

            };

            new Thread(runnable).start();

            CuratorWatcher observerNotifiedWatcher = event -> {
                LOGGER.debug("observer data updated: {}", event);
                remainingToNotify.countDown();
            };

            for (String observer : observers) {
                client.getData().usingWatcher(observerNotifiedWatcher).forPath(observersPath + "/" + observer);
            }

            // update data
            client.setData().forPath(path, Serializer.serialize(data));

        } catch(Exception e) {
            LOGGER.error("exception while notifying observers: {}", e);
            asyncTask.completeExceptionally(e);

        }

        return asyncTask;

    }

    private String generateUniqueChildPath() {
        return observersPath + "/" + hostname + observers.size();
    }

}

