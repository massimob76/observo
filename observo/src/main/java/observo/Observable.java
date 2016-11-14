package observo;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface Observable<T> {

    void registerObserver(Observer<T> observer);

    void unregisterObserver(Observer<T> observer);

    void unregisterAllObservers();

    void notifyObservers() throws InterruptedException, ExecutionException, TimeoutException;

    void notifyObservers(T data) throws InterruptedException, ExecutionException, TimeoutException;

    AsyncTask notifyObserversAsync();

    AsyncTask notifyObserversAsync(T data);
}
