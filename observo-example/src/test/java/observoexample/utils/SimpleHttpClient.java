package observoexample.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SimpleHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttpClient.class);

    private final ObjectMapper mapper;

    public SimpleHttpClient() {
        mapper = new ObjectMapper();
    }

    public <T> T get(String url, Class<T> objType) throws IOException {
        LOGGER.debug("Sending GET request to {} ", url);

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        assertThat(responseCode, is(200));

        T response = mapper.readValue(con.getInputStream(), objType);

        LOGGER.debug("Got as response {}", response);

        return response;
    }

    public void post(String url, Object obj) throws IOException {
        LOGGER.debug("Sending POST request to {} with object {}", url, obj);

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        byte[] valueAsBytes = mapper.writeValueAsBytes(obj);
        con.setDoOutput(true);

        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.write(valueAsBytes);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        assertThat(responseCode, is(200));

    }

}
