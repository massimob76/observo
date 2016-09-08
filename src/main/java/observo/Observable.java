package observo;

interface Observable<T> {

    void registerObserver(Observer<T> observer);

    void unregisterObserver(Observer observer);

    void notifyObservers() throws Exception;

    void notifyObservers(T data) throws Exception;
}
