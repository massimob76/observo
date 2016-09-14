package observo;

interface Observable<T> {

    void registerObserver(Observer<T> observer);

    void unregisterObserver(Observer<T> observer);

    void notifyObservers();

    void notifyObservers(T data);
}
