package observo.conf;

public class ObservoConf {

    private final long notificationTimeoutMs;
    private final long lockTimeoutMs;

    public ObservoConf(long notificationTimeoutMs, long lockTimeoutMs) {
        this.notificationTimeoutMs = notificationTimeoutMs;
        this.lockTimeoutMs = lockTimeoutMs;
    }

    public long getNotificationTimeoutMs() {
        return notificationTimeoutMs;
    }

    public long getLockTimeoutMs() {
        return lockTimeoutMs;
    }
}
