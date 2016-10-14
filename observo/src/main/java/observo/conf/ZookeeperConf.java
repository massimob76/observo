package observo.conf;

public class ZookeeperConf {

    private final String connectString;
    private final int connectionTimeoutMs;
    private final int retryTimes;
    private final int retryMsSleep;

    public ZookeeperConf(String connectString, int connectionTimeoutMs, int retryTimes, int retryMsSleep) {
        this.connectString = connectString;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.retryTimes = retryTimes;
        this.retryMsSleep = retryMsSleep;
    }

    public String getConnectString() {
        return connectString;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public int getRetryMsSleep() {
        return retryMsSleep;
    }
}
