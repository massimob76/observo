package observo;

import observo.conf.ObservoConf;
import observo.conf.ZookeeperConf;
import observo.utils.HostnameProvider;
import observo.utils.HostnameProviderImpl;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

import java.io.Serializable;

public class ObservableFactory {

    private static final String NAMESPACE_PREFIX = "observo";
    private static ObservableFactory factory;

    private final CuratorFramework client;
    private final RetryNTimes retryPolicy;
    private final String hostname;
    private final ObservoConf observoConf;

    private ObservableFactory(ZookeeperConf zookeeperConf, ObservoConf observoConf, String nameSpaceSuffix, HostnameProvider hostnameProvider) {
        this.observoConf = observoConf;
        retryPolicy = new RetryNTimes(zookeeperConf.getRetryTimes(), zookeeperConf.getRetryMsSleep());
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(zookeeperConf.getConnectionTimeoutMs())
                .namespace(NAMESPACE_PREFIX + "/" + nameSpaceSuffix)
                .connectString(zookeeperConf.getConnectString())
                .retryPolicy(retryPolicy)
                .build();

        client.start();

        this.client = client;
        this.hostname = hostnameProvider.getHostname();
    }

    public static ObservableFactory instance(ZookeeperConf zookeeperConf, ObservoConf observoConf, String nameSpaceSuffix) {
        return instance(zookeeperConf, observoConf, nameSpaceSuffix, new HostnameProviderImpl());
    }

    public static ObservableFactory instance(ZookeeperConf zookeeperConf, ObservoConf observoConf, String nameSpaceSuffix, HostnameProvider hostnameProvider) {
        if (factory == null) {
            factory = new ObservableFactory(zookeeperConf, observoConf, nameSpaceSuffix, hostnameProvider);
        }
        return factory;
    }

    public <T extends Serializable> Observable<T> createObservable(String name, Class<T> dataType) {
        String path = "/" + name;
        return new ObservableImpl<>(client, observoConf, hostname, path, dataType);
    }

}
