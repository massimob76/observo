package observo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostnameProviderImpl implements HostnameProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostnameProviderImpl.class);

    @Override
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.error("Could not get hostname: {}", e);
            throw new RuntimeException(e);
        }
    }
}
