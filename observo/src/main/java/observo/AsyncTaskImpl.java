package observo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AsyncTaskImpl implements AsyncTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncTaskImpl.class);

    private volatile Runnable completeTask;
    private volatile Consumer<Throwable> errorTask;
    private volatile boolean hasCompletedSuccessfully = false;
    private volatile boolean hasCompletedExceptionally = false;
    private volatile Throwable exception = null;
    private final AtomicBoolean oneOffToggle = new AtomicBoolean(false);
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void join(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (!latch.await(timeout, unit)) {
            throw new TimeoutException("Timeout reached " + timeout + " " + unit);
        }
        if (hasCompletedExceptionally) {
            throw new ExecutionException(exception);
        }
    }

    @Override
    public AsyncTaskImpl whenComplete(Runnable completeTask) {
        this.completeTask = completeTask;
        if (hasCompletedSuccessfully) {
            executeCompleteTask();
        }
        return this;
    }

    public void completeSuccessfully() {
        hasCompletedSuccessfully = true;
        executeCompleteTask();
    }

    @Override
    public AsyncTaskImpl whenError(Consumer<Throwable> errorTask) {
        this.errorTask = errorTask;
        if (hasCompletedExceptionally) {
            executeErrorTask();
        }
        return this;
    }

    public void completeExceptionally(Throwable ex) {
        this.exception = ex;
        hasCompletedExceptionally = true;
        executeErrorTask();
    }

    private void executeCompleteTask() {
        if (shouldExecuteTask()) {
            if (completeTask != null) {
                try {
                    completeTask.run();
                } catch(Exception e) {
                    LOGGER.error("Exception while running complete task: {}", completeTask, e);
                }
            }
            latch.countDown();
        }
    }

    private void executeErrorTask() {
        if (shouldExecuteTask()) {
            if (errorTask != null) {
                try {
                    errorTask.accept(exception);
                } catch (Exception e) {
                    LOGGER.error("Exception while running error task {}", errorTask, e);
                }
            }
            latch.countDown();
        }
    }

    private boolean shouldExecuteTask() {
        return oneOffToggle.compareAndSet(false, true);
    }

}
