package observoexample.news;

import observo.Observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NewsObserver implements Observer<News> {

    private volatile News latest;
    private List<News> newsReceivedSoFar = new CopyOnWriteArrayList<>();

    @Override
    public void update(News news) {
        latest = news;
        newsReceivedSoFar.add(news);
    }

    public News getLatest() {
        return latest;
    }

    public List<News> getNewsReceivedSoFar() {
        return newsReceivedSoFar;
    }
}
