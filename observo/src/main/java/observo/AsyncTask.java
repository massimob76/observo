package observo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public interface AsyncTask {

    void join(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    AsyncTask whenComplete(Runnable completeTask);

    AsyncTask whenError(Consumer<Throwable> errorTask);

}
