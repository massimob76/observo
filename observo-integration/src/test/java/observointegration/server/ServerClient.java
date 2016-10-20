package observointegration.server;

import observointegration.news.News;
import observointegration.utils.SimpleHttpClient;

import java.io.IOException;
import java.util.List;

public class ServerClient {

    private static final SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
    private static final String LATEST = "/news/latest";
    private static final String LATEST_SECOND = "/news/latest-second";
    private static final String ALL = "/news/all";
    private static final String PUBLISH_ASYNCH = "/news/publish-asynch";
    private static final String PUBLISH_SYNCH = "/news/publish-synch";

    private final ServerConnUrls serverConnUrls;

    public ServerClient(ServerConnUrls serverConnUrls) {
        this.serverConnUrls = serverConnUrls;
    }

    public List<News> getAllNews() throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getLoadBalancerUrl() + ALL, List.class);
    }

    public News getLatestNews() throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getLoadBalancerUrl() + LATEST, News.class);
    }

    public News getLatestNewsSecond() throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getLoadBalancerUrl() + LATEST_SECOND, News.class);
    }

    public News getLatestNewsFromServer(int serverNo) throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getServerInstanceUrl(serverNo) + LATEST, News.class);
    }

    public void publishNews(News news) throws IOException {
        simpleHttpClient.post("http://" + serverConnUrls.getLoadBalancerUrl() + PUBLISH_ASYNCH, news);
    }

    public void publishNewsSynch(News news) throws IOException {
        simpleHttpClient.post("http://" + serverConnUrls.getLoadBalancerUrl() + PUBLISH_SYNCH, news);
    }

    public void publishNewsToServer(News news, int serverNo) throws IOException {
        simpleHttpClient.post("http://" + serverConnUrls.getServerInstanceUrl(serverNo) + PUBLISH_ASYNCH, news);
    }
}
