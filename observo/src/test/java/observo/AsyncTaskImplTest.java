package observo;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class AsyncTaskImplTest {

    private static final Exception TEST_EXCEPTION = new RuntimeException("exception");
    private boolean successfullyCompleted = false;
    private Throwable errorCompletedException = null;
    private AsyncTaskImpl asyncTask;


    @Before
    public void setUp() {
        Runnable completeTask = () -> successfullyCompleted = true;
        Consumer<Throwable> errorTask = ex -> errorCompletedException = ex;
        asyncTask = new AsyncTaskImpl()
                .whenComplete(completeTask)
                .whenError(errorTask);
    }

    @Test
    public void onCompletionShouldRunTheCompletionTask() {
        asyncTask.completeSuccessfully();
        assertThat(successfullyCompleted, is(true));
    }

    @Test
    public void onErrorShouldRunTheErrorTask() {
        asyncTask.completeExceptionally(TEST_EXCEPTION);
        assertThat(errorCompletedException, is(TEST_EXCEPTION));
    }

    @Test
    public void joinShouldReturnOnCompletion() throws ExecutionException, InterruptedException, TimeoutException {
        new Thread(() -> asyncTask.completeSuccessfully()).start();
        asyncTask.join(100, TimeUnit.MILLISECONDS);
        assertThat(successfullyCompleted, is(true));
    }

    @Test
    public void joinShouldThrowExecutionExceptionOnErrorCompletion() throws ExecutionException, InterruptedException, TimeoutException {
        new Thread(() -> asyncTask.completeExceptionally(TEST_EXCEPTION)).start();
        try {
            asyncTask.join(100, TimeUnit.MILLISECONDS);
            fail();
        } catch (ExecutionException ex) {
            assertThat(ex.getCause(), is(TEST_EXCEPTION));
        }
        assertThat(errorCompletedException, is(TEST_EXCEPTION));
    }

    @Test(expected = TimeoutException.class)
    public void joinShouldThrowTimeoutExceptionOnTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        asyncTask.join(100, TimeUnit.MILLISECONDS);
    }

}