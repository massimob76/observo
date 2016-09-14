package server;

import observo.Observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NewsObserver implements Observer<News> {

    private volatile News latest;
    private List<News> soFarReceivedNews = new CopyOnWriteArrayList<>();

    @Override
    public void update(News news) {
        latest = news;
        soFarReceivedNews.add(news);
    }

    public News getLatest() {
        return latest;
    }

    public List<News> getSoFarReceivedNews() {
        return soFarReceivedNews;
    }
}
