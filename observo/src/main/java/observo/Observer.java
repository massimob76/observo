package observo;

public interface Observer<T> {

    void update(T data);

}