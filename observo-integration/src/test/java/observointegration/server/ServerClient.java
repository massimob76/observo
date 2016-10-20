package observointegration.server;

import com.fasterxml.jackson.core.type.TypeReference;
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
        return simpleHttpClient.get("http://" + serverConnUrls.getLoadBalancerUrl() + ALL, new TypeReference<List<News>>() {});
    }

    public News getLatestNews() throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getLoadBalancerUrl() + LATEST, new TypeReference<News>() {});
    }

    public News getLatestNewsSecond() throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getLoadBalancerUrl() + LATEST_SECOND, new TypeReference<News>() {});
    }

    public News getLatestNewsFromServer(int serverNo) throws IOException {
        return simpleHttpClient.get("http://" + serverConnUrls.getServerInstanceUrl(serverNo) + LATEST, new TypeReference<News>() {});
    }

    public void publishNewsAsynch(News news) throws IOException {
        simpleHttpClient.post("http://" + serverConnUrls.getLoadBalancerUrl() + PUBLISH_ASYNCH, news);
    }

    public void publishNewsSynch(News news) throws IOException {
        simpleHttpClient.post("http://" + serverConnUrls.getLoadBalancerUrl() + PUBLISH_SYNCH, news);
    }

    public void publishNewsToServer(News news, int serverNo) throws IOException {
        simpleHttpClient.post("http://" + serverConnUrls.getServerInstanceUrl(serverNo) + PUBLISH_ASYNCH, news);
    }
}
