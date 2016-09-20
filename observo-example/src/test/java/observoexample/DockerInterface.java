package observoexample;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class DockerInterface {

    private static final int NO_OF_SERVERS = 3;

    private int[] ports;

    public void startServers() throws IOException, InterruptedException {
        File observoExampleDir = getObservoExampleDirectory();

        Process p = Runtime.getRuntime().exec("./example-server.sh start " + NO_OF_SERVERS, null, observoExampleDir);
        p.waitFor();

        p = Runtime.getRuntime().exec("./example-server.sh ports " + NO_OF_SERVERS, null, observoExampleDir);
        p.waitFor();

        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String portsString = reader.readLine();
        reader.close();

        ports = Arrays.stream(portsString.split(" ")).mapToInt(s -> Integer.parseInt(s)).toArray();
    }

    public void stopServers() throws IOException, InterruptedException {
        File observoExampleDir = getObservoExampleDirectory();

        Process p = Runtime.getRuntime().exec("./example-server.sh stop", null, observoExampleDir);
        p.waitFor();
    }

    public void startSpecificServer(int serverNo) throws IOException, InterruptedException {
        File observoExampleDir = getObservoExampleDirectory();

        Process p = Runtime.getRuntime().exec("./example-server.sh startSpecificServer " + serverNo, null, observoExampleDir);
        p.waitFor();
    }

    public void stopSpecificServer(int serverNo) throws IOException, InterruptedException {
        File observoExampleDir = getObservoExampleDirectory();

        Process p = Runtime.getRuntime().exec("./example-server.sh stopSpecificServer " + serverNo, null, observoExampleDir);
        p.waitFor();
    }

    public int[] getPorts() {
        return ports;
    }

    private File getObservoExampleDirectory() {
        String currentDir = System.getProperty("user.dir");
        return new File(currentDir + "/observo-example");
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        DockerInterface dockerInterface = new DockerInterface();
        System.out.println("about to start everything");
        dockerInterface.startServers();
        System.out.println("portsString: " + Arrays.toString(dockerInterface.getPorts()));

        Thread.sleep(5000);
        System.out.println("about to stop server 2");
        dockerInterface.stopSpecificServer(2);

        Thread.sleep(5000);
        System.out.println("about to start server 2");
        dockerInterface.startSpecificServer(2);

        Thread.sleep(5000);
        System.out.println("about to stop everything");
        dockerInterface.stopServers();
    }
}
