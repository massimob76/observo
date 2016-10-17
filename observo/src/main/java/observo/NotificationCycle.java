package observo;

import observo.lock.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

public class NotificationCycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationCycle.class);

    private final Runnable onSuccess;
    private final Runnable onError;
    private final Runnable onCompletion;
    private final DistributedLock distributedLock;

    public enum State { STARTED, FAILED, SUCCESSFUL }

    private AtomicReference<State> currentState;

    public NotificationCycle(Runnable onSuccess, Runnable onError, Runnable onCompletion, DistributedLock distributedLock) {
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onCompletion = onCompletion;
        this.distributedLock = distributedLock;
        this.currentState = new AtomicReference<>(State.STARTED);

        distributedLock.acquireLock();
    }

    public State getCurrentState() {
        return currentState.get();
    }

    public void success() {
        boolean stateChange = currentState.compareAndSet(State.STARTED, State.SUCCESSFUL);
        if (stateChange) {
            distributedLock.releaseLock();
            safeRun(onSuccess);
            safeRun(onCompletion);
        }
    }

    public void failure() {
        boolean stateChange = currentState.compareAndSet(State.STARTED, State.FAILED);
        if (stateChange) {
            distributedLock.releaseLock();
            safeRun(onError);
            safeRun(onCompletion);
        }
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
