package observo.utils;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

public class CurrentTimeProviderTest {

    @Test
    public void getCurrentTimeMillis() {
        long beforeTimeMillis = System.currentTimeMillis();
        long currentTimeMillis = new CurrentTimeProvider().getCurrentTimeMillis();
        long afterTimeMillis = System.currentTimeMillis();

        assertThat(currentTimeMillis, greaterThanOrEqualTo(beforeTimeMillis));
        assertThat(currentTimeMillis,lessThanOrEqualTo(afterTimeMillis));
    }

}