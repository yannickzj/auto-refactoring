package refactor.analysis.examples;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MainTest {

    private int num = 10;

    @Test
    public void testInt() {
        int expected = 10;
        assertEquals(expected, num);
    }
}
