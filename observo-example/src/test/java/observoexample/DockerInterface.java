package observoexample;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class DockerInterface {

    private int[] ports;
    private int totalNoServers;

    public void startServers(int totalNoServers) throws IOException, InterruptedException {
        this.totalNoServers = totalNoServers;
        File observoExampleDir = getObservoExampleDirectory();

        Process p = Runtime.getRuntime().exec("./example-server.sh start " + totalNoServers, null, observoExampleDir);
        p.waitFor();

        p = Runtime.getRuntime().exec("./example-server.sh ports " + totalNoServers, null, observoExampleDir);
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

    public String getLoadBalancerHostPort() {
        return "localhost:80";
    }

    public String getSpecificHostPort(int serverNo) {
        return "localhost:" + ports[serverNo - 1];
    }

    private File getObservoExampleDirectory() {
        String currentDir = System.getProperty("user.dir");
        return new File(currentDir + "/observo-example");
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        int noOfServers = 3;
        DockerInterface dockerInterface = new DockerInterface();
        System.out.println("about to start everything");
        dockerInterface.startServers(noOfServers);
        System.out.println("server 1: " + dockerInterface.getSpecificHostPort(1));
        System.out.println("server 2: " + dockerInterface.getSpecificHostPort(2));
        System.out.println("server 3: " + dockerInterface.getSpecificHostPort(3));

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
