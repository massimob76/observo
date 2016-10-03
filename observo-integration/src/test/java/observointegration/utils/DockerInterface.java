package observointegration.utils;

import observointegration.server.ServerConnUrls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.function.UnaryOperator;

public class DockerInterface {

    private static final String SCRIPT_NAME = "integration-server.sh";
    private static final UnaryOperator<String> HOST_AND_PORT_FUNC = port -> "localhost:" + port;
    private static final int DELAY = 1000;

    private static ServerConnUrls serverConnUrls;

    public static void startEnvironment(int totalNoServers) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(getScriptPath() + " start " + totalNoServers);
        p.waitFor();

        String ports = getLastLine(p.getInputStream());
        String[] hostAndPorts = Arrays.stream(ports.split(" ")).map(HOST_AND_PORT_FUNC).toArray(String[]::new);
        serverConnUrls = new ServerConnUrls(HOST_AND_PORT_FUNC.apply("80"), hostAndPorts);

        Thread.sleep(DELAY);

    }

    public static void stopEnvironment() throws IOException, InterruptedException {
        String script = getScriptPath();

        Process p = Runtime.getRuntime().exec(script + " stop");
        p.waitFor();
        serverConnUrls = null;
    }

    public static void startSpecificServer(int serverNo) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(getScriptPath() + " startSpecificServer " + serverNo);
        p.waitFor();

        String port = getLastLine(p.getInputStream());
        String hostPort = HOST_AND_PORT_FUNC.apply(port);
        serverConnUrls.updateServerInstanceUrl(serverNo, hostPort);
    }

    public static void stopSpecificServer(int serverNo) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(getScriptPath() + " stopSpecificServer " + serverNo);
        p.waitFor();
    }

    public static ServerConnUrls getServerConnUrls() {
        return serverConnUrls;
    }

    private static String getScriptPath() {
        URL resource = DockerInterface.class.getClassLoader().getResource(SCRIPT_NAME);
        return resource.getPath();
    }

    private static String getLastLine(InputStream inputStream) throws IOException {
        String last = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String temp;
            while ((temp = reader.readLine()) != null) {
                last = temp;
            }
        }
        return last;
    }


}
