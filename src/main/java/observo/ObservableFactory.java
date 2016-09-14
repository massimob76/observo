package observo;

import observo.conf.ZookeeperConf;
import observo.utils.HostnameProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

import java.io.Serializable;

public class ObservableFactory {

    private static final String NAMESPACE_PREFIX = "observo";

    private final CuratorFramework client;
    private final RetryNTimes retryPolicy;
    private final String hostname;


    public ObservableFactory(ZookeeperConf zookeeperConf, String nameSpaceSuffix, HostnameProvider hostnameProvider) {
        retryPolicy = new RetryNTimes(zookeeperConf.getRetryTimes(), zookeeperConf.getRetryMsSleep());
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .namespace(NAMESPACE_PREFIX + "/" + nameSpaceSuffix)
                .connectString(zookeeperConf.getConnectString())
                .retryPolicy(retryPolicy)
                .build();

        client.start();

        this.client = client;
        this.hostname = hostnameProvider.getHostname();
    }

    public <T extends Serializable> Observable<T> createObservable(String name, Class<T> dataType) {
        String path = "/" + name;
        return new ObservableImpl<>(client, hostname, path, dataType);
    }

}
