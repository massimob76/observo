package observointegration;

import observointegration.news.News;
import observointegration.server.ServerClient;
import observointegration.utils.DockerInterface;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static observointegration.news.News.generateNewsForTesting;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NewsPropagatationITest {

    private static ServerClient serverClient;

    @Before
    public void setUp() {
        serverClient = new ServerClient(DockerInterface.getServerConnUrls());
    }

    @Test
    public void newsPropagate() throws IOException {
        News news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNews(), is(news));
        assertThat(serverClient.getLatestNewsSecond(), is(news));
    }

    @Test
    public void multipleNewsPropagate_withSynchPublishing() throws IOException {
        News news;

        for (int i = 0; i < 50; i++) {
            news = generateNewsForTesting();
            serverClient.publishNewsSynch(news);
            assertThat(serverClient.getLatestNews(), is(news));
        }

    }

    @Test
    public void multipleNewsProgate_withAsynchPublishing() throws IOException, InterruptedException {
        List<News> sent = new ArrayList<>();
        News news;

        for (int i = 0; i < 50; i++) {
            news = generateNewsForTesting();
            serverClient.publishNewsAsynch(news);
            sent.add(news);
        }

        Thread.sleep(500);
        List<News> received = serverClient.getAllNews();
        assertThat("not all news were received: sent: " + sent + " received: " + received, received.containsAll(sent), is(true));

    }

    @Test
    public void eachServerReceivesTheSameNews() throws IOException {
        News news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNewsFromServer(1), is(news));
        assertThat(serverClient.getLatestNewsFromServer(2), is(news));
        assertThat(serverClient.getLatestNewsFromServer(3), is(news));
    }

}
