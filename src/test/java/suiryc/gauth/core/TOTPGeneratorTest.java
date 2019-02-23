package suiryc.gauth.core;

import java.nio.charset.StandardCharsets;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TOTPGeneratorTest {

    private static byte[] SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void generator() throws Exception {
        // See https://tools.ietf.org/html/rfc4226#page-32
        assertEquals("755224", TOTPGenerator.generate(SECRET, 0));
        assertEquals("287082", TOTPGenerator.generate(SECRET, 1));
        assertEquals("359152", TOTPGenerator.generate(SECRET, 2));
        assertEquals("969429", TOTPGenerator.generate(SECRET, 3));
        assertEquals("338314", TOTPGenerator.generate(SECRET, 4));
        assertEquals("254676", TOTPGenerator.generate(SECRET, 5));
        assertEquals("287922", TOTPGenerator.generate(SECRET, 6));
        assertEquals("162583", TOTPGenerator.generate(SECRET, 7));
        assertEquals("399871", TOTPGenerator.generate(SECRET, 8));
        assertEquals("520489", TOTPGenerator.generate(SECRET, 9));
        // Some more values to check '0' padding on the left.
        assertEquals("026920", TOTPGenerator.generate(SECRET, 30));
        assertEquals("003784", TOTPGenerator.generate(SECRET, 36));
    }

}
