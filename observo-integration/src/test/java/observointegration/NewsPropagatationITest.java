package observointegration;

import observointegration.news.News;
import observointegration.server.ServerClient;
import observointegration.utils.DockerInterface;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

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
    public void multipleNewsPropagate() throws IOException {
        News news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNewsSynch(news);
        assertThat(serverClient.getLatestNews(), is(news));

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
