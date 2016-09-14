package observo.conf;

public class ZookeeperConf {

    private final String connectString;
    private final int retryTimes;
    private final int retryMsSleep;

    public ZookeeperConf(String connectString, int retryTimes, int retryMsSleep) {
        this.connectString = connectString;
        this.retryTimes = retryTimes;
        this.retryMsSleep = retryMsSleep;
    }

    public String getConnectString() {
        return connectString;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getRetryMsSleep() {
        return retryMsSleep;
    }
}
