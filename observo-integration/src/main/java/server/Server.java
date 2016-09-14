package server;

import observo.Observable;
import observo.ObservableFactory;
import observo.conf.ObservoConf;
import observo.conf.ZookeeperConf;
import observo.utils.HostnameProvider;

public class Server {

    private static final int RETRY_TIMES = 2;
    private static final int RETRY_MS_SLEEP = 100;
    private static final long NOTIFICATION_TIMEOUT_MS = 100;
    private static final long LOCK_TIMEOUT_MS = 100;

    public Server(final String hostname, String connectionString) {
        ZookeeperConf zookeeperConf = new ZookeeperConf(connectionString, RETRY_TIMES, RETRY_MS_SLEEP);
        ObservoConf observoConf = new ObservoConf(NOTIFICATION_TIMEOUT_MS, LOCK_TIMEOUT_MS);
        String nameSpaceSuffix = "myapp";
        HostnameProvider hostnameProvider = () -> hostname;
        ObservableFactory observableFactory = new ObservableFactory(zookeeperConf, observoConf, nameSpaceSuffix, hostnameProvider);

        Observable<News> observable = observableFactory.createObservable("news", News.class);


        observable.registerObserver(new NewsObserver());


    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("please provide hostname");
        }
    }
}
