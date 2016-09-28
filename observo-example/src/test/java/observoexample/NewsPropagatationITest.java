package observoexample;

import observoexample.news.News;
import observoexample.utils.DockerInterface;
import observoexample.server.ServerClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static observoexample.news.News.generateNewsForTesting;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NewsPropagatationITest {

    private static final int NO_OF_SERVERS = 3;
    private static DockerInterface dockerInterface;
    private static ServerClient serverClient;

    @BeforeClass
    public static void startServers() throws IOException, InterruptedException {
        dockerInterface = new DockerInterface();
        dockerInterface.startEnvironment(NO_OF_SERVERS);
        serverClient = new ServerClient(dockerInterface.getServerConnUrls());
    }

    @AfterClass
    public static void stopServers() throws IOException, InterruptedException {
        dockerInterface.stopEnvironment();
    }

    @Test
    public void newsPropagate() throws IOException {
        News news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNews(), is(news));
        assertThat(serverClient.getLatestNewsSecond(), is(news));
    }

    @Test
    public void multipleNewsPropagate() throws IOException {
        News news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNews(), is(news));

        news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNews(), is(news));

    }

    @Test
    public void eachServerReceivesTheSameNews() throws IOException {
        News news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNewsFromServer(1), is(news));
        assertThat(serverClient.getLatestNewsFromServer(2), is(news));
        assertThat(serverClient.getLatestNewsFromServer(3), is(news));
    }

}
