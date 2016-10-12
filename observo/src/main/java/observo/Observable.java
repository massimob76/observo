package observo;

public interface Observable<T> {

    void registerObserver(Observer<T> observer);

    void unregisterObserver(Observer<T> observer);

    void unregisterAllObservers();

    void notifyObservers();

    void notifyObservers(T data);

    void notifyObservers(Runnable onSuccess, Runnable onError, Runnable onCompletion);

    void notifyObservers(T data, Runnable onSuccess, Runnable onError, Runnable onCompletion);
}
