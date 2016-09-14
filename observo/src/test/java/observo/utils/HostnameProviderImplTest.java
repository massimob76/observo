package observo.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class HostnameProviderImplTest {

    private HostnameProviderImpl hostnameProvider = new HostnameProviderImpl();

    @Test
    public void getHostname() {
        assertThat(hostnameProvider.getHostname(), is(not(nullValue())));
    }

}