package observo;

import observo.utils.Serializer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class ObserverWatcher<T extends Serializable> implements CuratorWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObserverWatcher.class);


    private final CuratorFramework client;
    private final String path;
    private final String childPath;
    private final Observer<T> observer;
    private final Class<T> dataType;
    private volatile boolean enabled = true;

    public ObserverWatcher(CuratorFramework client, String path, String childPath, Observer<T> observer, Class<T> dataType) throws Exception {
        this.client = client;
        this.path = path;
        this.childPath = childPath;
        this.observer = observer;
        this.dataType = dataType;
        createNodePath();
    }

    public void disable() throws Exception {
        deleteNodePath();
        enabled = false;
    }

    @Override
    public void process(WatchedEvent event) throws Exception {

        // 1. collect data
        LOGGER.debug("data change detected");

        if (enabled) {
            byte[] data = client.getData().forPath(path);

            // 2. call observer with data
            observer.update(Serializer.deserialize(data, dataType));

            // 3. set watcher on data
            client.getData().usingWatcher(this).forPath(path);

            // 4. modify child observer data
            client.setData().forPath(childPath);

        } else {
            LOGGER.debug("watcher is disabled; no action will be performed");
        }
    }

    private void createNodePath() throws Exception {
        if (client.checkExists().forPath(childPath) == null) {
            client.create().forPath(childPath);
        }
    }

    private void deleteNodePath() throws Exception {
        client.delete().forPath(childPath);
    }

}
