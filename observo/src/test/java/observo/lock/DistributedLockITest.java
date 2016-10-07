package observo.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.curator.test.TestingServer;
import org.hamcrest.core.Is;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DistributedLockITest {

    private static final int RETRY_TIMES = 1;
    private static final int RETRY_MS_SLEEP = 10;
    private static final String NAMESPACE = "observo/testApp";
    private static final long LOCK_TIMEOUT_MS = 100;

    private static TestingServer zkServer;
    private static DistributedLock distributedLock;


    @BeforeClass
    public static void setUpClass() throws Exception {
        zkServer = new TestingServer();

        RetryNTimes retryPolicy = new RetryNTimes(RETRY_TIMES, RETRY_MS_SLEEP);
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .namespace(NAMESPACE)
                .connectString(zkServer.getConnectString())
                .retryPolicy(retryPolicy)
                .build();

        client.start();

        distributedLock = new DistributedLock(client, "/lockpath", LOCK_TIMEOUT_MS);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        zkServer.stop();
    }

    @Test
    public void acquireLock() {
        distributedLock.acquireLock();
        assertThat(distributedLock.isLocked(), is(true));
    }

    @Test
    public void releaseLock() {
        distributedLock.releaseLock();
        assertThat(distributedLock.isLocked(), Is.is(false));
    }

    @Test
    public void lockedBlock() {
        class TestRunnable implements Runnable {

            public volatile boolean didRun = false;

            @Override
            public void run() {
                assertThat(distributedLock.isLocked(), Is.is(true));
                didRun = true;
            }
        }

        TestRunnable runnable = new TestRunnable();
        distributedLock.lockedBlock(runnable);

        assertThat(runnable.didRun, Is.is(true));
        assertThat(distributedLock.isLocked(), Is.is(false));
    }

    @Test
    public void lockIsReleasedWhenBlockThrowsException() {
        try {
            distributedLock.lockedBlock(() -> {
                throw new RuntimeException();
            });

            fail();

        } catch(RuntimeException e) {
            assertThat(distributedLock.isLocked(), Is.is(false));
        }

    }

}