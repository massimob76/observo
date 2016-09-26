package observoexample;

import observoexample.news.News;
import observoexample.server.ServerClient;
import observoexample.utils.DockerInterface;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static observoexample.news.News.generateNewsForTesting;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NewsPropagationServerFailureITest {

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
//        dockerInterface.stopEnvironment();
    }

    @Test
    public void oneServerDownDoesNotCompromiseOthers() throws IOException, InterruptedException {
        dockerInterface.stopSpecificServer(2);
        News news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNewsFromServer(1), is(news));
        assertThat(serverClient.getLatestNewsFromServer(3), is(news));

        dockerInterface.startSpecificServer(2);
        Thread.sleep(5000);
        news = generateNewsForTesting();
        serverClient.publishNews(news);
        assertThat(serverClient.getLatestNewsFromServer(1), is(news));
        assertThat(serverClient.getLatestNewsFromServer(2), is(news));
        assertThat(serverClient.getLatestNewsFromServer(3), is(news));
    }

}
