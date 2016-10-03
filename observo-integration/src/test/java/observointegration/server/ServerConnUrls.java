package observointegration.server;

public class ServerConnUrls {

    private final String loadBalancerUrl;
    private final String[] serverInstanceUrls;

    public ServerConnUrls(String loadBalancerUrl, String... serverInstanceUrls) {
        this.loadBalancerUrl = loadBalancerUrl;
        this.serverInstanceUrls = serverInstanceUrls;
    }

    public String getLoadBalancerUrl() {
        return loadBalancerUrl;
    }

    public String getServerInstanceUrl(int serverNo) {
        if (serverNo < 1 || serverNo > serverInstanceUrls.length) {
            throw new RuntimeException("Out of bounds server url! Requested url for server " + serverNo + " but having " + serverInstanceUrls.length + " servers");
        }
        return serverInstanceUrls[serverNo - 1];
    }

    public void updateServerInstanceUrl(int serverNo, String serverInstanceUrl) {
        this.serverInstanceUrls[serverNo - 1] = serverInstanceUrl;
    }
}
