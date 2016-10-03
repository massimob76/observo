package observointegration;

import observointegration.news.News;
import observointegration.server.ServerClient;
import observointegration.utils.DockerInterface;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static observointegration.news.News.generateNewsForTesting;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NewsPropagationServerFailureITest {

    private static ServerClient serverClient;

    @Before
    public void setUp() {
        serverClient = new ServerClient(DockerInterface.getServerConnUrls());
    }

    @Test
    public void oneServerDownDoesNotCompromiseOthers() throws IOException, InterruptedException {
        DockerInterface.stopSpecificServer(2);
        News news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNewsFromServer(1), is(news));
        assertThat(serverClient.getLatestNewsFromServer(3), is(news));

        DockerInterface.startSpecificServer(2);
        Thread.sleep(5000);
        news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNewsFromServer(1), is(news));
        assertThat(serverClient.getLatestNewsFromServer(2), is(news));
        assertThat(serverClient.getLatestNewsFromServer(3), is(news));
    }

}
