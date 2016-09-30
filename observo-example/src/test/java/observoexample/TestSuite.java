package observoexample;

import observoexample.utils.DockerInterface;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;

@RunWith(Suite.class)
@Suite.SuiteClasses({NewsPropagatationITest.class, NewsPropagationServerFailureITest.class})
public class TestSuite {

    private static final int NO_OF_SERVERS = 3;

    @BeforeClass
    public static void startServers() throws IOException, InterruptedException {
        DockerInterface.startEnvironment(NO_OF_SERVERS);
    }

    @AfterClass
    public static void stopServers() throws IOException, InterruptedException {
        DockerInterface.stopEnvironment();
    }
}
