package observo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AfterNotificationAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AfterNotificationAction.class);

    private final CountDownLatch remaining;

    public AfterNotificationAction(int noOfObservers, long notificationTimeoutMs, Runnable onSuccess, Runnable onError, Runnable onCompletion) {
        this.remaining = new CountDownLatch(noOfObservers);

        Runnable runnable = () -> {
            boolean notifiedToAll = false;
            try {
                notifiedToAll = remaining.await(notificationTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                LOGGER.error("Nofication was interrupted {}", e);
            }

            if (notifiedToAll) {
                LOGGER.info("observers were successfully notified");
                safeRun(onSuccess);
            } else {
                LOGGER.error("could not notify all the observers within {} ms", notificationTimeoutMs);
                safeRun(onError);
            }

            safeRun(onCompletion);
        };

        new Thread(runnable).start();
    }

    public void observerNotified() {
        remaining.countDown();
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
