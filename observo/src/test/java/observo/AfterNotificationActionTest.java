package observo;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AfterNotificationActionTest {

    private static final int NO_OF_OBSERVERS = 2;
    private static final long NOTIFICATION_TIMEOUT_MS = 100;

    private static class TestRunnable implements Runnable {

        private CountDownLatch latch = new CountDownLatch(1);

        public boolean didRun() throws InterruptedException {
            return latch.await(150, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run() {
            latch.countDown();
        }
    }

    private final TestRunnable onSuccess = new TestRunnable();
    private final TestRunnable onError = new TestRunnable();
    private final TestRunnable onCompletion = new TestRunnable();

    @Test
    public void runOnSuccessAndOnCompletionWhenReceivingAllNotifications() throws InterruptedException {
        AfterNotificationAction action = new AfterNotificationAction(NO_OF_OBSERVERS, NOTIFICATION_TIMEOUT_MS, onSuccess, onError, onCompletion);
        action.observerNotified();
        action.observerNotified();
        assertThat(onSuccess.didRun(), is(true));
        assertThat(onError.didRun(), is(false));
        assertThat(onCompletion.didRun(), is(true));
    }

    @Test
    public void runOnErrorAndOnCompletionWhenNotReceivingAllNotifications() throws InterruptedException {
        AfterNotificationAction action = new AfterNotificationAction(NO_OF_OBSERVERS, NOTIFICATION_TIMEOUT_MS, onSuccess, onError, onCompletion);
        action.observerNotified();
        assertThat(onSuccess.didRun(), is(false));
        assertThat(onError.didRun(), is(true));
        assertThat(onCompletion.didRun(), is(true));
    }

    @Test
    public void copesWithNull() throws InterruptedException {
        AfterNotificationAction action = new AfterNotificationAction(NO_OF_OBSERVERS, NOTIFICATION_TIMEOUT_MS, null, onError, null);
        action.observerNotified();
        action.observerNotified();
        assertThat(onError.didRun(), is(false));

        action = new AfterNotificationAction(NO_OF_OBSERVERS, NOTIFICATION_TIMEOUT_MS, onSuccess, null, null);
        action.observerNotified();
        assertThat(onSuccess.didRun(), is(false));
    }

    @Test
    public void executesCompletionEvenOnExceptions() throws InterruptedException {
        Runnable runnableException = () -> {
            throw new RuntimeException();
        };
        new AfterNotificationAction(NO_OF_OBSERVERS, NOTIFICATION_TIMEOUT_MS, runnableException, runnableException, onCompletion);
        assertThat(onCompletion.didRun(), is(true));
    }

}