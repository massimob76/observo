package observo;

import observo.conf.ZookeeperConf;
import observo.utils.CurrentTimeProvider;
import observo.utils.Serializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.WatchedEvent;

import java.io.Serializable;

public class ObservableFactory {

    private static final String NAMESPACE_PREFIX = "observo";

    private final CuratorFramework client;
    private final int coolDownPeriodMs;
    private final CurrentTimeProvider currentTimeProvider;


    public ObservableFactory(ZookeeperConf zookeeperConf, String nameSpaceSuffix, int coolDownPeriodMs, CurrentTimeProvider currentTimeProvider) {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .namespace(NAMESPACE_PREFIX + "/" + nameSpaceSuffix)
                .connectString(zookeeperConf.getConnectString())
                .retryPolicy(new RetryNTimes(zookeeperConf.getRetryTimes(), zookeeperConf.getRetryMsSleep()))
                .build();

        client.start();

        this.client = client;
        this.coolDownPeriodMs = coolDownPeriodMs;
        this.currentTimeProvider = currentTimeProvider;
    }

    public <T extends Serializable> Observable<T> createObservable(String name, Class<T> dataType) throws Exception {
        String path = "/" + name;
        createPathIfNotExist(path);

        return new Observable<T>() {

            private long lastNotify = -1;

            @Override
            public void registerObserver(Observer<T> observer) {
                CuratorWatcher curatorWatcher = new CuratorWatcher() {
                    @Override
                    public void process(WatchedEvent event) throws Exception {
                        byte[] data = client.getData().forPath(path);
                        observer.update(Serializer.deserialize(data, dataType));
                        client.getData().usingWatcher(this).forPath(path);
                    }
                };

                try {
                    client.getData().usingWatcher(curatorWatcher).forPath(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void unregisterObserver(Observer observer) {
                // TODO
            }

            @Override
            public void notifyObservers() throws Exception {
                notifyObservers(null);
            }

            @Override
            public void notifyObservers(T data) throws Exception {
                coolDown();
                client.setData().forPath(path, Serializer.serialize(data));
                this.lastNotify = currentTimeProvider.getCurrentTimeMillis();
            }

            private void coolDown() throws InterruptedException {
                if (lastNotify != -1) {
                    long awaitFor = coolDownPeriodMs - currentTimeProvider.getCurrentTimeMillis() + lastNotify;
                    if (awaitFor > 0) {
                        Thread.sleep(awaitFor);
                    }
                }
            }
        };

    }

    private void createPathIfNotExist(String path) throws Exception {
        if (this.client.checkExists().forPath(path) == null) {
            this.client.create().forPath(path);
        }
    }
}
